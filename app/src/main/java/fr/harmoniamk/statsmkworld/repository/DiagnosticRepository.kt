package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.CandidateRoster
import fr.harmoniamk.statsmkworld.model.local.MissingPlayer
import fr.harmoniamk.statsmkworld.model.local.MkworldCandidate
import fr.harmoniamk.statsmkworld.model.local.OpponentResolution
import fr.harmoniamk.statsmkworld.model.local.UnknownOpponentDiagnostic
import fr.harmoniamk.statsmkworld.model.local.UnresolvedOpponent
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outils de diagnostic debug (`DebugViewModel`) : arbitrage des adversaires « Équipe
 * inconnue » et des joueurs manquants, sur les wars historiques Firebase. Repository
 * dédié (agrège Firebase/MKCentral/Room/DataStore, un seul consommateur — rule 32).
 */
interface DiagnosticRepositoryInterface {
    suspend fun diagnoseUnknownOpponents(): List<UnknownOpponentDiagnostic>
    suspend fun reattributeOpponent(hostRosterId: String, warId: Long, rawId: String, newId: String)
    suspend fun deleteWar(hostRosterId: String, warId: Long)
    suspend fun diagnoseMissingPlayers(): List<MissingPlayer>
    suspend fun addMissingPlayerAsAlly(playerId: String)
}

@Module
@InstallIn(SingletonComponent::class)
interface DiagnosticRepositoryModule {
    @Binds
    @Singleton
    fun bindRepository(impl: DiagnosticRepository): DiagnosticRepositoryInterface
}

class DiagnosticRepository @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : DiagnosticRepositoryInterface {

    // Override manuel `rawId (War.teamOpponent) → teamId mkworld cible`, établi à la main
    // quand l'heuristique nom/tag ne suffit pas. Prioritaire sur l'heuristique (candidats
    // = tous les rosters de l'équipe cible). Aucune réattribution auto : l'humain confirme.
    private val opponentOverrides: Map<String, String> = mapOf(
        "3149" to "27",
        "3168" to "903",
        "1623" to "885",
        "2784" to "2606",
        "1000" to "3497", // Code Galaxy
        "1874" to "885",  // Race in the Space (même équipe cible que 1623 — normal)
        "3943" to "7",    // Rozando la Katastrofe (3943 = roster du quasi-doublon 3182 → vraie équipe teamId 7)
        "3996" to "1783", // Nakama Clan
    )

    // Diagnostic NON destructif (aucune écriture) : retient les wars dont un teamOpponent
    // ne résout AUCUNE TeamEntity locale, charge une seule fois les équipes mkworld
    // (rule 31), puis résout chaque id distinct en mémoire (évite N appels réseau).
    override suspend fun diagnoseUnknownOpponents(): List<UnknownOpponentDiagnostic> {
        val hostRosterIds = dataStoreRepository.mkcTeam.firstOrNull()
            ?.rosters?.filter { it.game == "mkworld" }?.map { it.id.toString() }
            .orEmpty()

        // Collecte des wars à adversaire non résolu (getTeam == null → inconnu).
        val warsWithUnknown = mutableListOf<Triple<String, War, List<String>>>()
        hostRosterIds.forEach { hostId ->
            firebaseRepository.getWars(hostId).forEach { war ->
                val unresolvedIds = war.teamOpponent.filter { databaseRepository.getTeam(it) == null }
                if (unresolvedIds.isNotEmpty())
                    warsWithUnknown.add(Triple(hostId, war, unresolvedIds))
            }
        }

        // Chargement unique des équipes mkworld actives 6+ joueurs (réutilisées en mémoire).
        val mkworldTeams = fetchAllMkworldTeams()

        // Résolution mutualisée : chaque id distinct n'est résolu qu'une fois.
        val distinctIds = warsWithUnknown.flatMap { it.third }.toHashSet()
        val resolutions = distinctIds.associateWith { rawId ->
            resolveOpponentId(rawId, mkworldTeams)
        }

        return warsWithUnknown.map { (hostId, war, unresolvedIds) ->
            val details = WarDetails(war)
            UnknownOpponentDiagnostic(
                hostRosterId = hostId,
                warId = war.id,
                teamHost = war.teamHost,
                date = details.date,
                displayedScore = when (war.teamOpponent.size > 1) {
                    // 24p : scores (WarScore triés desc.), informatif dans le diagnostic.
                    true -> details.scores.joinToString(" - ") { it.score.toString() }
                    else -> details.displayedScore
                },
                unresolvedOpponents = unresolvedIds.map { rawId ->
                    UnresolvedOpponent(rawId, resolutions[rawId] ?: OpponentResolution.Error)
                }
            )
        }
    }

    // Toutes les pages de getTeams (équipes mkworld, même filtre que la synchro registre).
    // Renvoie null si un appel échoue → l'appelant en déduit une résolution Error.
    private suspend fun fetchAllMkworldTeams(): List<MKCTeam>? {
        val first = mkCentralDataSource.getTeams(1).successResponse ?: return null
        val teams = first.teamList.toMutableList()
        var page = 1
        while (page < first.pageCount) {
            page++
            val next = mkCentralDataSource.getTeams(page).successResponse ?: return null
            teams.addAll(next.teamList)
        }
        return teams
    }

    // Résout rawId dans la liste mkworld chargée, puis propose des candidats mkworld :
    //  - override manuel prioritaire (teamId mappé → tous ses rosters ; retombe sur
    //    l'heuristique si le teamId cible est absent de la liste) ;
    //  - heuristique nom/tag : source (rawId == roster.id ou teamId), puis équipes dont
    //    le tag OU le nom matche (sous-chaîne insensible à la casse, deux sens).
    // Id absent tombe en NotFound sauf override. Le rosterId candidat est l'id à réécrire.
    private fun resolveOpponentId(
        rawId: String,
        mkworldTeams: List<MKCTeam>?
    ): OpponentResolution {
        if (mkworldTeams == null) return OpponentResolution.Error

        // Override manuel prioritaire : équipe cible = teamId mappé, tous ses rosters mkworld.
        opponentOverrides[rawId]?.let { targetTeamId ->
            mkworldTeams.firstOrNull { it.id.toString() == targetTeamId }?.let { target ->
                return OpponentResolution.Found(
                    teamId = target.id.toString(),
                    teamName = target.name,
                    teamTag = target.tag,
                    mkworldCandidates = listOf(mkworldCandidate(target))
                )
            }
            // teamId cible absent de la liste (hors actives 6+) → on retombe sur l'heuristique.
        }

        val source = mkworldTeams.firstOrNull { team ->
            team.id.toString() == rawId || team.rosters.any { it.id.toString() == rawId }
        } ?: return OpponentResolution.NotFound

        val nameQuery = source.name.lowercase()
        val tagQuery = source.tag.lowercase()
        val candidates = mkworldTeams
            .filter { team -> team.rosters.any { it.game == "mkworld" } }
            .filter { team ->
                val teamTag = team.tag.lowercase()
                val teamName = team.name.lowercase()
                // Tag = signal le plus fiable ; nom en complément. Match bidirectionnel.
                teamTag.contains(tagQuery) || tagQuery.contains(teamTag) ||
                    teamName.contains(nameQuery) || nameQuery.contains(teamName)
            }
            .map { mkworldCandidate(it) }

        return OpponentResolution.Found(
            teamId = source.id.toString(),
            teamName = source.name,
            teamTag = source.tag,
            mkworldCandidates = candidates
        )
    }

    // Équipe candidate + ses rosters mkworld (rosterId = id à réécrire).
    private fun mkworldCandidate(team: MKCTeam) = MkworldCandidate(
        teamId = team.id.toString(),
        teamName = team.name,
        teamTag = team.tag,
        rosters = team.rosters
            .filter { it.game == "mkworld" }
            .map { CandidateRoster(rosterId = it.id.toString(), name = it.name, tag = it.tag) }
    )

    // Réécrit teamOpponent (rawId → newId), UNIQUEMENT si newId se résout localement
    // (rule 12 — ne jamais écrire un id non résolvable).
    override suspend fun reattributeOpponent(hostRosterId: String, warId: Long, rawId: String, newId: String) {
        if (databaseRepository.getTeam(newId) != null) {
            firebaseRepository.getWars(hostRosterId).firstOrNull { it.id == warId }?.let { war ->
                val migrated = war.teamOpponent.map { if (it == rawId) newId else it }
                if (migrated != war.teamOpponent)
                    firebaseRepository.writeWar(hostRosterId, war.copy(teamOpponent = migrated))
            }
        }
    }

    // Retire une war irrécupérable du nœud hôte Firebase (stats réhydratées au prochain fetch).
    override suspend fun deleteWar(hostRosterId: String, warId: Long) {
        firebaseRepository.deleteWar(hostRosterId, warId.toString())
    }

    // Diagnostic NON destructif des joueurs manquants : playerId des wars absents du cache
    // local (membres + alliés), comptés par joueur, résolus via MKCentral. Id non résolu
    // → dégradé (« Joueur inconnu ») sans faire disparaître l'entrée.
    override suspend fun diagnoseMissingPlayers(): List<MissingPlayer> {
        val hostRosterIds = dataStoreRepository.mkcTeam.firstOrNull()
            ?.rosters?.filter { it.game == "mkworld" }?.map { it.id.toString() }
            .orEmpty()

        // playerId → nombre de wars distinctes où il apparaît.
        val warCountByPlayerId = mutableMapOf<String, Int>()
        hostRosterIds.forEach { hostId ->
            firebaseRepository.getWars(hostId).forEach { war ->
                war.tracks.flatMap { it.positions }.map { it.playerId }.toHashSet()
                    .forEach { playerId ->
                        warCountByPlayerId[playerId] = (warCountByPlayerId[playerId] ?: 0) + 1
                    }
            }
        }

        // Ne garde que les ids absents du cache local (membres + alliés).
        val missingIds = warCountByPlayerId.keys
            .filter { databaseRepository.getPlayer(it).firstOrNull() == null }

        return missingIds.map { playerId ->
            val player = mkCentralDataSource.getPlayer(playerId).successResponse
            MissingPlayer(
                playerId = playerId,
                name = player?.name ?: "Joueur inconnu",
                country = player?.countryCode.orEmpty(),
                warCount = warCountByPlayerId[playerId] ?: 0
            )
        }
    }

    // Ajoute un allié en local ET sur Firebase newAllies (les deux, sinon la resynchro
    // fetchAllies effacerait l'allié local). Un allié a toujours role=0 (rosterId "-1").
    override suspend fun addMissingPlayerAsAlly(playerId: String) {
        mkCentralDataSource.getPlayer(playerId).successResponse?.let { player ->
            databaseRepository.addAlly(PlayerEntity(player = player, isAlly = true))
            dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
                firebaseRepository.writeAlly(team.id.toString(), User(player))
            }
        }
    }

}
