package fr.harmoniamk.statsmkworld.usecase

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.firebase.Tag
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamList
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.SeasonRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

interface FetchUseCaseInterface {
    suspend fun fetchData(playerId: String)
    suspend fun fetchPlayer(playerId: String): MKCPlayer?
    suspend fun fetchTeam(teamId: String): MKCTeam?
    suspend fun fetchAllies(teamId: String)
    suspend fun fetchTeams(): String
    suspend fun fetchWars(teamId: String)
    suspend fun fetchTags()
    fun manageTransferts(): Flow<Unit>
    fun migrateOpponentsToRoster(): Flow<Unit>
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface FetchUseCaseModule {
    @Binds
    @Singleton
    fun bindRepository(impl: FetchUseCase): FetchUseCaseInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class FetchUseCase @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val seasonRepository: SeasonRepositoryInterface
) : FetchUseCaseInterface, CoroutineScope {

    override suspend fun fetchData(playerId: String) = fetchPlayer(playerId)
        ?.rosters?.firstOrNull { it.game == "mkworld" }
        ?.let {
            val team = fetchTeam(it.teamID.toString())
            fetchAllies(team?.id.toString())
            fetchTeams()
            val rostersId = team?.rosters?.filter { it.game == "mkworld" }?.map { it.id.toString() }
            rostersId?.forEach { fetchWars(it) }
            // Saisons (#30) : RTDB seasons/{teamId} → Room (rattaché à l'ÉQUIPE, pas au roster).
            team?.id?.let { seasonRepository.fetchSeasons(it.toString()) }
            dataStoreRepository.setLastUpdate(Date().time)
        } ?: Unit

    override suspend fun fetchPlayer(playerId: String): MKCPlayer? {
        val player = mkCentralDataSource.getPlayer(playerId).successResponse
        player?.let {
            dataStoreRepository.setMKCPlayer(it)
        }
        return player
    }

    override suspend fun fetchTeam(teamId: String): MKCTeam? {
        val team = mkCentralDataSource.getTeam(teamId).successResponse
        team?.let {
            dataStoreRepository.setMKCTeam(it)
            databaseRepository.clearPlayers()
            // Avatar des membres (#50) : seul registry/players/{id} le porte (pas les endpoints
            // équipe) → résolu PAR membre, SÉQUENTIELLEMENT (rule 30 : une rafale parallèle se
            // fait throttler par MKCentral → successResponse=null sans exception). Chaque appel
            // tolérant aux échecs (runCatching → avatar null = initiales), tous traités pareil.
            it.rosters.filter { roster -> roster.game == "mkworld" }.forEach { roster ->
                roster.players.forEach { player ->
                    val user = runCatching { firebaseRepository.getUser(teamId, player.playerId) }.getOrNull()
                    val avatar = runCatching {
                        mkCentralDataSource.getPlayer(player.playerId).successResponse
                            ?.userSettings?.avatar?.takeIf { avatar -> avatar.isNotEmpty() }
                    }.getOrNull()
                    val playerEntity = PlayerEntity(player = player, role = user?.role ?: 0, currentWar = user?.currentWar.orEmpty(), discordId = user?.discordId.orEmpty(), rosterId = roster.id.toString(), avatar = avatar)
                    databaseRepository.writePlayer(playerEntity)
                }
            }
        }
        return team
    }

    override suspend fun fetchAllies(teamId: String) {
        val allies = firebaseRepository.getAllies(teamId)
        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        allies.forEach { ally ->
            when (players.map { it.id }.contains(ally.id)) {
                true -> {
                    databaseRepository.getPlayer(ally.id).firstOrNull()?.let { player ->
                        firebaseRepository.deleteAlly(teamId, ally.id)
                        databaseRepository.updateUserRoster(ally.id, player.rosterId)
                    }
                }

                else -> {
                    mkCentralDataSource.getPlayer(ally.id).successResponse?.let {
                        databaseRepository.addAlly(PlayerEntity(player = it, isAlly = true))
                    }
                }
            }
        }
    }

    override suspend fun fetchTeams(): String  {
        // Domaine exclusivement mkworld (rule 31) : on ne récupère/stocke QUE des équipes mkworld.
        val teams = mutableListOf<TeamEntity>()
        var teamPage = 1
        val firstResponse = getTeams(teamPage)
        // TeamEntity(MKCTeam) porte aussi ses rosters {id, nom, tag} (déjà dans la réponse liste).
        teams.addAll(firstResponse.second?.map { TeamEntity(it) }.orEmpty())
        while (teamPage < (firstResponse.first ?: 1)) {
            teamPage++
            val teamsToAdd = getTeams(teamPage)
            teams.addAll(teamsToAdd.second?.map { TeamEntity(it) }.orEmpty())
        }
        // Purge + réécriture : le cache reflète EXACTEMENT la récupération courante (flushe les
        // reliquats périmés → doublons de registre). GARDE-FOU anti-wipe : on ne purge que si la
        // page 1 est revenue non nulle (sinon rien, pour ne pas effacer sur erreur réseau).
        firstResponse.second?.let {
            databaseRepository.clearTeams()
            // Pas d'équipe sans roster mkworld (non résoluble à la granularité roster).
            databaseRepository.writeTeams(teams.filter { it.rosters.isNotEmpty() })
            // « 6v6 Squad » (wars amicales sans adversaire MKCentral) conservée hors filtre roster.
            databaseRepository.writeTeams(listOf(
                TeamEntity(
                    name = "6v6 Squad",
                    tag = "SQ",
                    id = "123456789",
                    color = null,
                    logo = null
                )
            ))
        }
        return dataStoreRepository.mkcTeam.firstOrNull()?.id.toString()
    }

    override suspend fun fetchWars(teamId: String) {
        val wars = firebaseRepository.getWars(teamId)
        databaseRepository.clearWars()
        databaseRepository.writeWars(wars.map { WarEntity(it) })
    }
    override suspend fun fetchTags() {
        val tags = databaseRepository.getTeams().map { it.map { Tag(it.tag, it.id) } }.firstOrNull()
        tags?.let { firebaseRepository.writeTags(it) }
    }

    override fun manageTransferts() = dataStoreRepository.mkcTeam
        .map { mkCentralDataSource.getTeam(it.id.toString()).successResponse }
        .zip(databaseRepository.getPlayers()) { team, players ->
            players.forEach { player ->
                if (team?.rosters?.firstOrNull { it.game == "mkworld" }?.players?.none { it.playerId == player.id } == true) {
                    mkCentralDataSource.getPlayer(player.id).successResponse?.let { mkcPlayer ->
                        val fbUser = firebaseRepository.getUser(team.id.toString(), player.id)
                        fbUser?.let {
                            firebaseRepository.writeUser(mkcPlayer.rosters?.firstOrNull { it.game == "mkworld" }?.teamID.toString(), it)
                            firebaseRepository.writeAlly(team.id.toString(), it)
                            databaseRepository.updateUserRoster(it.id, rosterId = "-1")
                            firebaseRepository.deleteUser(team.id.toString(), it.id)
                        }
                    }
                }
                if (team?.rosters?.filter { it.game == "mkworld" }?.flatMap { it.players }?.any { it.playerId == player.id } == true) {
                    mkCentralDataSource.getPlayer(player.id).successResponse?.let { mkcPlayer ->
                        val fbUser = User(mkcPlayer)
                        firebaseRepository.writeUser(team.id.toString(), fbUser)
                        firebaseRepository.deleteAlly(team.id.toString(), fbUser.id)
                        databaseRepository.updateUserRoster(fbUser.id, rosterId = team.rosters.firstOrNull { it.game == "mkworld" && it.players.map { it.playerId }.contains(mkcPlayer.id.toString()) }?.id.toString())
                    }
                }
            }
        }


    // Migration idempotente teamId → rosterId des adversaires des wars historiques
    // (wars/{host}/{warId} ; currentWars exclu). Pour chaque id de War.teamOpponent :
    //  - teamId d'une équipe MONO-roster mkworld → remplacé par ce rosterId ;
    //  - équipe multi-rosters (roster joué inconnu) ou rosterId déjà → laissé tel quel.
    // Garde-fou : remplacement seulement si le rosterId cible se résout (getTeam != null) —
    // sinon le nom/logo disparaîtrait à l'affichage. War réécrite uniquement si teamOpponent
    // change réellement (idempotent).
    override fun migrateOpponentsToRoster() = dataStoreRepository.mkcTeam
        .map { it.rosters.filter { roster -> roster.game == "mkworld" }.map { roster -> roster.id.toString() } }
        .zip(databaseRepository.getTeams()) { hostRosterIds, teams ->
            // teamId → rosterId pour les équipes mono-roster dont le rosterId cible se résout.
            val monoRosterMap = teams
                .filter { it.rosters.size == 1 }
                .mapNotNull { team ->
                    val rosterId = team.rosters.first().id
                    when (databaseRepository.getTeam(rosterId)) {
                        null -> null
                        else -> team.id to rosterId
                    }
                }
                .toMap()
            hostRosterIds.forEach { hostId ->
                firebaseRepository.getWars(hostId).forEach { war ->
                    val migrated = war.teamOpponent.map { opponentId ->
                        monoRosterMap[opponentId] ?: opponentId
                    }
                    if (migrated != war.teamOpponent)
                        firebaseRepository.writeWar(hostId, war.copy(teamOpponent = migrated))
                }
            }
        }


    private suspend fun getTeams(page: Int): Pair<Int?, MKCTeamList?> {
        val teams = mkCentralDataSource.getTeams(page).successResponse
        return Pair(teams?.pageCount, teams?.teamList)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

}