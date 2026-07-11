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
 * Orchestration des outils de **diagnostic debug** (écran `DebugScreen` /
 * `DebugViewModel`) : arbitrage des adversaires « Équipe inconnue » et des
 * joueurs manquants, sur les wars historiques Firebase.
 *
 * Vit dans un repository dédié (et non dans `FetchUseCase`) car cette logique
 * n'est consommée que par **un seul** appelant (`DebugViewModel`) — cf. rule
 * `.claude/rules/32-usecase-vs-repository.md`. Il agrège plusieurs sources
 * (Firebase, MKCentral, Room, DataStore), d'où un repository dédié plutôt qu'un
 * repository mono-source.
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

    // Override manuel EXPERT pour le diagnostic des adversaires « Équipe inconnue ».
    // Correspondances `rawId (War.teamOpponent) → teamId mkworld cible` établies à
    // la main par l'équipe à partir de ses données historiques, quand l'heuristique
    // nom/tag ne suffit pas. Cette table PREND LE PAS sur la recherche heuristique :
    // les candidats sont alors construits depuis l'équipe cible (tous ses rosters
    // mkworld). Aucune réattribution automatique — l'humain confirme via l'écran.
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

    // Diagnostic NON destructif (Étape 0 du ticket adversaires « Équipe inconnue »).
    // 1. Balaye les wars de chaque roster hôte (wars/{hostRosterId}) et retient
    //    celles dont un id de teamOpponent ne se résout à AUCUNE TeamEntity locale
    //    (même échec que War.opponentTeams → « Équipe inconnue »).
    // 2. Charge UNE SEULE FOIS la liste des équipes mkworld actives, non
    //    historiques et à effectif ≥ 6 (toutes pages via le MÊME endpoint que la
    //    synchro registre, getTeams — miroir du filtre par défaut du site
    //    MKCentral), puis résout chaque id distinct en mémoire — évite N appels
    //    réseau. Domaine exclusivement mkworld (cf. rule 31-mkworld-only) : aucun
    //    accès mk8dx.
    // 3. Pour chaque id : retrouve l'équipe « source » mkworld (rawId == roster.id
    //    ou == teamId), puis rebondit sur son nom/tag pour proposer des candidats
    //    mkworld (l'adversaire a souvent recréé une équipe avec un nom/tag proche).
    //    Un id introuvable dans cette liste (équipe < 6 joueurs, dissoute, ou
    //    d'origine mk8dx pure) non couvert par l'override manuel tombe en NotFound
    //    (voulu : ces cas sont dans la table d'override ou supprimés).
    // Aucune écriture : sert uniquement à produire le rapport d'arbitrage.
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

    // Charge toutes les pages de getTeams (équipes mkworld actives, non
    // historiques, ≥ 6 joueurs — même endpoint et même filtre que la synchro
    // registre, miroir du filtre par défaut du site MKCentral).
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

    // Résout un id source (rawId) dans la liste mkworld déjà chargée (équipes
    // actives 6+ joueurs), puis propose des candidats mkworld. Domaine exclusivement
    // mkworld (cf. rule 31-mkworld-only).
    //  - OVERRIDE MANUEL prioritaire : si rawId est dans opponentOverrides, on
    //    cible directement le teamId mappé (recherché dans mkworldTeams) et on
    //    liste TOUS ses rosters mkworld comme candidats. Si ce teamId cible est
    //    absent de la liste (équipe hors périmètre actives 6+), on retombe sur
    //    l'heuristique.
    //  - HEURISTIQUE nom/tag : match source rawId == roster.id OU teamId (mkworld
    //    uniquement), puis depuis son name/tag on retient les équipes mkworld dont
    //    le tag OU le nom matche (sous-chaîne insensible à la casse, deux sens —
    //    cf. AddWarViewModel.onSearchTeam) et possédant un roster mkworld.
    // Un id absent de cette liste (adversaire dissous/historique/à faible effectif,
    // ou d'origine mk8dx pure) tombe en NotFound sauf s'il est couvert par l'override
    // manuel. Le rosterId d'un roster mkworld candidat est l'id à réécrire.
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

    // Une équipe mkworld candidate + ses rosters mkworld (rosterId = id à réécrire).
    // Partagé par la branche override manuelle et la branche heuristique nom/tag.
    private fun mkworldCandidate(team: MKCTeam) = MkworldCandidate(
        teamId = team.id.toString(),
        teamName = team.name,
        teamTag = team.tag,
        rosters = team.rosters
            .filter { it.game == "mkworld" }
            .map { CandidateRoster(rosterId = it.id.toString(), name = it.name, tag = it.tag) }
    )

    // Réattribution (paquet A) : réécrit teamOpponent en remplaçant rawId par
    // newId, UNIQUEMENT si newId se résout localement (rule 12 — ne jamais
    // écrire un id non résolvable). La war est réécrite sous son nœud hôte.
    override suspend fun reattributeOpponent(hostRosterId: String, warId: Long, rawId: String, newId: String) {
        if (databaseRepository.getTeam(newId) != null) {
            firebaseRepository.getWars(hostRosterId).firstOrNull { it.id == warId }?.let { war ->
                val migrated = war.teamOpponent.map { if (it == rawId) newId else it }
                if (migrated != war.teamOpponent)
                    firebaseRepository.writeWar(hostRosterId, war.copy(teamOpponent = migrated))
            }
        }
    }

    // Suppression (paquet B) : retire une war irrécupérable du nœud hôte Firebase.
    // La réhydratation des stats se fait au prochain fetch/InitStatsWorker.
    override suspend fun deleteWar(hostRosterId: String, warId: Long) {
        firebaseRepository.deleteWar(hostRosterId, warId.toString())
    }

    // Diagnostic NON destructif des JOUEURS manquants (miroir de
    // diagnoseUnknownOpponents). Collecte les playerId de toutes les wars des
    // rosters hôtes, retient ceux absents du cache local (membres + alliés),
    // dédoublonne, compte les wars par joueur, puis résout name/country via
    // MKCentral (un getPlayer par id distinct). Id non résolu → valeur dégradée
    // (« Joueur inconnu », pays vide) sans faire disparaître l'entrée. Lecture seule.
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

    // Ajoute un joueur manquant comme ALLIÉ, en local ET sur Firebase newAllies —
    // les deux, sinon la resynchro (fetchAllies) effacerait l'allié local. Un allié
    // a toujours role=0 (rosterId "-1" via PlayerEntity(isAlly = true)).
    override suspend fun addMissingPlayerAsAlly(playerId: String) {
        mkCentralDataSource.getPlayer(playerId).successResponse?.let { player ->
            databaseRepository.addAlly(PlayerEntity(player = player, isAlly = true))
            dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
                firebaseRepository.writeAlly(team.id.toString(), User(player))
            }
        }
    }

}
