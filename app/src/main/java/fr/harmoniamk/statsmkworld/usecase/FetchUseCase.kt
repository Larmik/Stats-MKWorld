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
    private val databaseRepository: DatabaseRepositoryInterface
) : FetchUseCaseInterface, CoroutineScope {

    override suspend fun fetchData(playerId: String) = fetchPlayer(playerId)
        ?.rosters?.firstOrNull { it.game == "mkworld" }
        ?.let {
            val team = fetchTeam(it.teamID.toString())
            fetchAllies(team?.id.toString())
            fetchTeams()
            val rostersId = team?.rosters?.filter { it.game == "mkworld" }?.map { it.id.toString() }
            rostersId?.forEach { fetchWars(it) }
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
            // L'endpoint liste d'équipe (MKCTeamPlayer) ne porte pas l'avatar des membres.
            // On enrichit au mieux le SEUL joueur courant depuis son profil DataStore
            // (mkcPlayer.userSettings.avatar). Les alliés récupèrent le leur via fetchAllies
            // (fetchés en MKCPlayer). #50 pt.4.
            val currentPlayer = dataStoreRepository.mkcPlayer.firstOrNull()
            val currentAvatar = currentPlayer?.userSettings?.avatar?.takeIf { avatar -> avatar.isNotEmpty() }
            databaseRepository.clearPlayers()
            it.rosters.filter { it.game == "mkworld" }.forEach { roster ->
                roster.players.forEach { player ->
                    val user = firebaseRepository.getUser(teamId, player.playerId)
                    val avatar = currentAvatar.takeIf { player.playerId == currentPlayer?.id?.toString() }
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
        // Domaine exclusivement mkworld (cf. rule 31-mkworld-only) : on ne récupère
        // et ne stocke QUE les équipes mkworld.
        val teams = mutableListOf<TeamEntity>()
        var teamPage = 1
        val firstResponse = getTeams(teamPage)
        // TeamEntity(MKCTeam) renseigne aussi rosters {id, nom, tag} (rosters mkworld) —
        // aucune requête supplémentaire, les rosters sont déjà dans la réponse liste.
        teams.addAll(firstResponse.second?.map { TeamEntity(it) }.orEmpty())
        while (teamPage < (firstResponse.first ?: 1)) {
            teamPage++
            val teamsToAdd = getTeams(teamPage)
            teams.addAll(teamsToAdd.second?.map { TeamEntity(it) }.orEmpty())
        }
        // Purge + réécriture : le cache reflète EXACTEMENT la récupération mkworld
        // courante (flushe tout reliquat périmé keyé par un ancien id — rosterId
        // d'un schéma antérieur, équipe mk8dx d'avant la purge — cause des doublons
        // de registre, ex. deux « Rozando la Katástrofe »). GARDE-FOU anti-wipe :
        // on ne purge QUE si la récupération réseau a réussi (page 1 renvoyée non
        // nulle) ; sinon on n'écrit rien pour ne pas effacer le registre sur une
        // erreur réseau.
        firstResponse.second?.let {
            databaseRepository.clearTeams()
            // Ne persiste pas une équipe sans roster mkworld : elle ne serait pas
            // résoluble à la granularité roster (et n'a pas de line-up mkworld à jouer).
            databaseRepository.writeTeams(teams.filter { it.rosters.isNotEmpty() })
            // L'équipe spéciale « 6v6 Squad » (wars amicales sans adversaire MKCentral)
            // est conservée volontairement après la purge, hors filtre roster (aucun
            // roster mkworld).
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


    // Migration idempotente teamId → rosterId des adversaires dans les wars
    // historiques (nœud wars/{host}/{warId}), pilotée par le cache local des
    // équipes. currentWars volontairement exclu.
    //
    // Pour chaque identifiant présent dans War.teamOpponent :
    //  - s'il correspond à un teamId d'une équipe possédant EXACTEMENT un roster
    //    mkworld → remplacé par ce rosterId (cas non ambigu) ;
    //  - équipe multi-rosters → conservé en teamId (roster joué inconnu) ;
    //  - déjà un rosterId → ne matche aucun teamId connu → laissé tel quel.
    // Garde-fou : on ne remplace un teamId par son rosterId que si ce rosterId
    // se résout effectivement à l'instant T (databaseRepository.getTeam(rosterId)
    // != null). Sinon on ne migre pas — écrire un rosterId non résolvable ferait
    // disparaître le nom/logo de l'adversaire à l'affichage. La war n'est réécrite
    // que si son teamOpponent a réellement changé (idempotent : une 2ᵉ exécution
    // ne produit aucune écriture).
    override fun migrateOpponentsToRoster() = dataStoreRepository.mkcTeam
        .map { it.rosters.filter { roster -> roster.game == "mkworld" }.map { roster -> roster.id.toString() } }
        .zip(databaseRepository.getTeams()) { hostRosterIds, teams ->
            // teamId → rosterId, uniquement pour les équipes mono-roster mkworld
            // dont le rosterId cible est résolvable localement (getTeam != null).
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