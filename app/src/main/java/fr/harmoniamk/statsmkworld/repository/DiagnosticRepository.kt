package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.mkWorldRosters
import fr.harmoniamk.statsmkworld.extension.opponentTeams
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.CandidateRoster
import fr.harmoniamk.statsmkworld.model.local.MissingPlayer
import fr.harmoniamk.statsmkworld.model.local.MkworldCandidate
import fr.harmoniamk.statsmkworld.model.local.OfficialWarCandidate
import fr.harmoniamk.statsmkworld.model.local.OpponentResolution
import fr.harmoniamk.statsmkworld.model.local.Tournament
import fr.harmoniamk.statsmkworld.model.local.UnknownOpponentDiagnostic
import fr.harmoniamk.statsmkworld.model.local.UnresolvedOpponent
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outils de diagnostic debug (`DebugViewModel`) : arbitrage des adversaires « Équipe
 * inconnue » et des joueurs manquants, migration rétroactive des wars officielles (#156),
 * sur les wars historiques Firebase. Repository
 * dédié (agrège Firebase/MKCentral/Room/DataStore, un seul consommateur).
 */
interface DiagnosticRepositoryInterface {
    suspend fun diagnoseUnknownOpponents(): List<UnknownOpponentDiagnostic>
    suspend fun reattributeOpponent(hostRosterId: String, warId: Long, rawId: String, newId: String)
    suspend fun deleteWar(hostRosterId: String, warId: Long)
    suspend fun diagnoseMissingPlayers(): List<MissingPlayer>
    suspend fun addMissingPlayerAsAlly(playerId: String)
    /** Wars amicales de l'équipe jouées à une date du calendrier officiel, en soirée (lecture seule). */
    suspend fun findOfficialWarCandidates(): List<OfficialWarCandidate>
    /** Écrit le `tournamentId` des [candidates] sur Firebase puis rafraîchit Room ; nb de wars migrées par tournoi. */
    suspend fun migrateOfficialWars(candidates: List<OfficialWarCandidate>): Map<Tournament, Int>
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
    // ne résout AUCUNE TeamEntity locale, charge une seule fois les équipes mkworld,
    // puis résout chaque id distinct en mémoire (évite N appels réseau).
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
    // (ne jamais écrire un id non résolvable).
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

    // Une war est officielle si sa date de création (Paris) figure au calendrier ET qu'elle a été
    // lancée à partir de 19h30 (aucun match officiel avant 20h ; tolère une création juste avant).
    // Les wars déjà rattachées à un tournoi ne sont jamais candidates (idempotent).
    override suspend fun findOfficialWarCandidates(): List<OfficialWarCandidate> {
        val parisZone = ZoneId.of("Europe/Paris")
        // Dates de match par saison (Atlas S1/S2/S3 partagent ATLAS_LEAGUE : la saison n'est pas stockée).
        val calendar: Map<LocalDate, Tournament> = listOf(
            // Atlas League S1
            Tournament.ATLAS_LEAGUE to listOf(
                "2025-10-12", "2025-10-19", "2025-10-26", "2025-11-02", "2025-11-09",
                "2025-11-16", "2025-11-23", "2025-11-30", "2025-12-07", "2025-12-14"
            ),
            // Atlas League S2
            Tournament.ATLAS_LEAGUE to listOf(
                "2026-03-15", "2026-03-22", "2026-03-29", "2026-04-12", "2026-04-19",
                "2026-04-26", "2026-05-03"
            ),
            // EuroLeague S1 (10 mai confirmé malgré le dimanche)
            Tournament.EUROLEAGUE to listOf(
                "2026-04-10", "2026-04-17", "2026-04-24", "2026-05-01", "2026-05-08",
                "2026-05-10", "2026-05-22"
            ),
            // MKCentral Frontier
            Tournament.MKC_FRONTIER to listOf("2026-06-06", "2026-06-07", "2026-06-13", "2026-06-14"),
            // Low Div Cup S18
            Tournament.LOW_DIV_CUP to listOf("2026-08-22", "2026-08-29", "2026-09-05", "2026-09-12", "2026-09-19"),
            // Atlas League S3
            Tournament.ATLAS_LEAGUE to listOf("2026-09-20", "2026-09-27"),
        ).flatMap { (tournament, dates) -> dates.map { LocalDate.parse(it) to tournament } }.toMap()

        val hostRosterIds = dataStoreRepository.mkcTeam.firstOrNull()
            ?.mkWorldRosters()?.map { it.id.toString() }
            .orEmpty()

        return hostRosterIds.flatMap { hostId ->
            firebaseRepository.getWars(hostId)
                .filter { it.tournamentId == null }
                .mapNotNull { war ->
                    val createdAt = Instant.ofEpochMilli(war.id).atZone(parisZone)
                    calendar[createdAt.toLocalDate()]
                        ?.takeIf { !createdAt.toLocalTime().isBefore(LocalTime.of(19, 30)) }
                        ?.let { tournament ->
                            OfficialWarCandidate(
                                hostRosterId = hostId,
                                war = war,
                                tournament = tournament,
                                createdAt = createdAt,
                                opponents = war.opponentTeams(databaseRepository)
                            )
                        }
                }
        }.sortedBy { it.war.id }
    }

    // Relit les wars Firebase (le `copy` porte sur la version fraîche, jamais sur une conversion
    // Room) et ne réécrit que celles encore sans tournoi. Room est ensuite rafraîchi en UNE passe
    // (un seul clear pour toutes les rosters, audit B27), sans relire Firebase après écriture.
    override suspend fun migrateOfficialWars(candidates: List<OfficialWarCandidate>): Map<Tournament, Int> {
        val tournamentByWarId = candidates.associate { it.war.id to it.tournament }
        val hostRosterIds = dataStoreRepository.mkcTeam.firstOrNull()
            ?.mkWorldRosters()?.map { it.id.toString() }
            .orEmpty()
        val warsByHost = hostRosterIds.associateWith { firebaseRepository.getWars(it) }

        val migratedWars = warsByHost.flatMap { (hostId, wars) ->
            wars.mapNotNull { war ->
                tournamentByWarId[war.id]
                    ?.takeIf { war.tournamentId == null }
                    ?.let { hostId to war.copy(tournamentId = it.name) }
            }
        }
        // Écritures séquentielles : volume faible, pas de rafale Firebase.
        migratedWars.forEach { (hostId, war) -> firebaseRepository.writeWar(hostId, war) }

        val migratedById = migratedWars.associate { (_, war) -> war.id to war }
        val refreshedWars = warsByHost.values.flatten().map { migratedById[it.id] ?: it }
        // Garde-fou anti-wipe : pas de clear si aucune war n'a pu être lue.
        if (refreshedWars.isNotEmpty()) {
            databaseRepository.clearWars()
            databaseRepository.writeWars(refreshedWars.map { WarEntity(it) })
        }
        return migratedWars.groupingBy { (_, war) -> tournamentByWarId.getValue(war.id) }.eachCount()
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
