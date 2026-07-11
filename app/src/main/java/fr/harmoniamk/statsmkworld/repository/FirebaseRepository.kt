package fr.harmoniamk.statsmkworld.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.parsePenalties
import fr.harmoniamk.statsmkworld.extension.parseScores
import fr.harmoniamk.statsmkworld.extension.parseTracks
import fr.harmoniamk.statsmkworld.extension.toMapList
import fr.harmoniamk.statsmkworld.model.firebase.Tag
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface FirebaseRepositoryInterface {
    //Auth
    suspend fun signInAnonymously(): Boolean
    fun isUserConnected(): Boolean

    //SplashScreen/Login
    suspend fun getUsers(teamId: String): List<User>
    suspend fun getUser(teamId: String, id: String): User?
    suspend fun writeUser(teamId: String, user: User)
    suspend fun updateUserCurrentWar(teamId: String, user: User)
    suspend fun deleteUser(teamId: String, id: String)

    suspend fun getWars(teamId: String): List<War>
    suspend fun writeWar(war: War)
    suspend fun writeWar(teamId: String, war: War)
    suspend fun deleteWar(teamId: String, warId: String)

    suspend fun getCurrentWar(teamId: String): War?
    fun listenToCurrentWar(teamId: String): Flow<War?>
    suspend fun writeCurrentWar(war: War)
    suspend fun deleteCurrentWar(teamId: String)
    suspend fun restoreCurrentWarIfHost(war: War?)

    suspend fun getAllies(teamId: String): List<User>
    suspend fun writeAlly(teamId: String, user: User)
    suspend fun updateAllyCurrentWar(teamId: String, user: User)
    suspend fun deleteAlly(teamId: String, ally: String)

    suspend fun log(message: String, type: String)
    suspend fun writeTags(tags: List<Tag>)
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface FirebaseRepositoryModule {
    @Binds
    @Singleton
    fun bindRepository(impl: FirebaseRepository): FirebaseRepositoryInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class FirebaseRepository @Inject constructor(private val dataStoreRepository: DataStoreRepositoryInterface) :
    FirebaseRepositoryInterface {

    private val database = Firebase.database.reference

    override suspend fun signInAnonymously(): Boolean = suspendCancellableCoroutine { cont ->
        Firebase.auth.signInAnonymously()
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { cont.resume(false) }
    }

    override fun isUserConnected(): Boolean = Firebase.auth.currentUser != null

    /** Attend la complétion d'un `get()` Firebase ; renvoie `null` en cas d'échec (pas de crash). */
    private suspend fun Task<DataSnapshot>.awaitSnapshot(): DataSnapshot? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resume(null) }
        }

    private suspend fun currentRosterId(): String? = dataStoreRepository.mkcPlayer
        .firstOrNull()
        ?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()

    override suspend fun getUser(teamId: String, id: String): User? = withContext(Dispatchers.IO) {
        (database.child("users").child(teamId).child(id).get().awaitSnapshot()?.value as? Map<*, *>)?.toUser()
    }

    override suspend fun getUsers(teamId: String): List<User> = withContext(Dispatchers.IO) {
        database.child("users").child(teamId).get().awaitSnapshot()?.children
            ?.mapNotNull { (it.value as? Map<*, *>)?.toUser() }
            .orEmpty()
    }

    override suspend fun getAllies(teamId: String): List<User> = withContext(Dispatchers.IO) {
        database.child("newAllies").child(teamId).get().awaitSnapshot()?.children
            ?.mapNotNull { (it.value as? Map<*, *>)?.toUser() }
            .orEmpty()
    }

    override suspend fun getWars(teamId: String): List<War> = withContext(Dispatchers.IO) {
        database.child("wars").child(teamId).get().awaitSnapshot()?.children
            ?.mapNotNull { (it.value as? Map<*, *>)?.toWar() }
            .orEmpty()
    }

    override suspend fun getCurrentWar(teamId: String): War? = withContext(Dispatchers.IO) {
        (database.child("currentWars").child(teamId).get().awaitSnapshot()?.value as? Map<*, *>)?.toWar()
    }

    override fun listenToCurrentWar(teamId: String): Flow<War?> = callbackFlow {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val war = (dataSnapshot.child("currentWars").child(teamId).value as? Map<*, *>)?.toWar()
                if (isActive) trySend(war)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        database.addValueEventListener(postListener)
        awaitClose { database.removeEventListener(postListener) }
    }.flowOn(Dispatchers.IO)

    override suspend fun writeUser(teamId: String, user: User) {
        database.child("users").child(teamId).child(user.id).setValue(user)
    }

  override suspend fun updateUserCurrentWar(teamId: String, user: User) = withContext(Dispatchers.IO) {
        val ref = database.child("users").child(teamId).child(user.id)
        if (ref.get().awaitSnapshot()?.value != null)
            ref.updateChildren(mapOf("currentWar" to user.currentWar))
        else ref.setValue(user)
        Unit
    }

    override suspend fun deleteUser(teamId: String, id: String) {
        database.child("users").child(teamId).child(id).removeValue()
    }

    override suspend fun writeWar(war: War) {
        currentRosterId()?.let { database.child("wars").child(it).child(war.id.toString()).setValue(war) }
    }

    // Écrit une war historique dans un nœud hôte explicite (wars/{teamId}/{warId}),
    // indépendamment du roster courant. Utilisé par la migration teamId→rosterId
    // déclenchée depuis l'écran Debug : chaque war est réécrite sous son propre
    // nœud hôte (war.teamHost), pas sous le roster de l'utilisateur courant.
    override suspend fun writeWar(teamId: String, war: War) {
        database.child("wars").child(teamId).child(war.id.toString()).setValue(war)
    }

    // Suppression d'une war historique irrécupérable (adversaire introuvable),
    // déclenchée manuellement depuis l'écran Debug après décision humaine.
    override suspend fun deleteWar(teamId: String, warId: String) {
        database.child("wars").child(teamId).child(warId).removeValue()
    }

    override suspend fun writeCurrentWar(war: War) {
        // Estampille le créateur (id MKCentral) au premier écrit ; on préserve
        // un playerHostId déjà présent (mises à jour de courses successives).
        val warToWrite = when (war.playerHostId) {
            0L -> war.copy(playerHostId = dataStoreRepository.mkcPlayer.firstOrNull()?.id ?: 0L)
            else -> war
        }
        currentRosterId()?.let { database.child("currentWars").child(it).setValue(warToWrite) }
    }

    override suspend fun deleteCurrentWar(teamId: String) {
        database.child("currentWars").child(teamId).removeValue()
    }

    /**
     * Réhydrate le DataStore war si celui-ci est vide alors que la war Firebase
     * a été créée par le joueur courant (playerHostId == mkcPlayer.id). Permet
     * au créateur de retrouver ses droits d'édition après un DataStore nettoyé
     * (logout, réinstallation, autre appareil). Sans effet si la war est nulle,
     * si le DataStore contient déjà une war, ou si le joueur courant n'est pas
     * le créateur (id absent → 0L, war legacy → playerHostId 0L).
     */
    override suspend fun restoreCurrentWarIfHost(war: War?) {
        war?.let {
            val hasLocalWar = dataStoreRepository.war.firstOrNull() != null
            val playerId = dataStoreRepository.mkcPlayer.firstOrNull()?.id ?: 0L
            if (!hasLocalWar && playerId != 0L && it.playerHostId == playerId) {
                dataStoreRepository.setCurrentWar(it)
            }
        }
    }

    override suspend fun writeAlly(teamId: String, user: User) {
        database.child("newAllies").child(teamId).child(user.id).setValue(user)
    }

    override suspend fun updateAllyCurrentWar(teamId: String, user: User) = withContext(Dispatchers.IO) {
        val ref = database.child("newAllies").child(teamId).child(user.id)
        if (ref.get().awaitSnapshot()?.value != null)
            ref.updateChildren(mapOf("currentWar" to user.currentWar))
        else ref.setValue(user)
        Unit
    }

    override suspend fun deleteAlly(teamId: String, ally: String) {
        database.child("newAllies").child(teamId).child(ally).removeValue()
    }

    override suspend fun log(message: String, type: String) {
        database.child("debug").child(Date().displayedString("dd-MM-yyyy")).child(type)
            .child(Date().time.toString()).setValue(message)
    }

    override suspend fun writeTags(tags: List<Tag>) {
        database.child("tags").setValue(tags)
    }

    private fun Map<*, *>.toUser() = User(
        id = this["id"].toString(),
        currentWar = this["currentWar"].toString(),
        role = this["role"].toString().toIntOrNull() ?: 0,
        name = this["name"].toString(),
        discordId = this["discordId"].toString()
    )

    private fun Map<*, *>.toWar() = War(
        id = this["id"].toString().toLong(),
        teamHost = this["teamHost"].toString(),
        teamOpponent = this["teamOpponent"] as List<String>,
        tracks = this["tracks"].toMapList().parseTracks().orEmpty(),
        penalties = this["penalties"].toMapList().parsePenalties().orEmpty(),
        scores = this["scores"].toMapList().parseScores().orEmpty(),
        // War legacy sans playerHostId → 0L (parsing null-safe, pas de crash).
        playerHostId = this["playerHostId"]?.toString()?.toLongOrNull() ?: 0L
    )

}
