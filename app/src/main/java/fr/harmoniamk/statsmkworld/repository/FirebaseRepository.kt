package fr.harmoniamk.statsmkworld.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
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
    //SplashScreen/Login
    suspend fun getUsers(teamId: String): List<User>
    suspend fun getUser(teamId: String, id: String): User?
    suspend fun writeUser(teamId: String, user: User)
    suspend fun updateUserCurrentWar(teamId: String, user: User)
    suspend fun deleteUser(teamId: String, id: String)

    suspend fun getWars(teamId: String): List<War>
    suspend fun writeWar(war: War)

    suspend fun getCurrentWar(teamId: String): War?
    fun listenToCurrentWar(teamId: String): Flow<War?>
    suspend fun writeCurrentWar(war: War)
    suspend fun deleteCurrentWar(teamId: String)

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

    override suspend fun writeCurrentWar(war: War) {
        currentRosterId()?.let { database.child("currentWars").child(it).setValue(war) }
    }

    override suspend fun deleteCurrentWar(teamId: String) {
        database.child("currentWars").child(teamId).removeValue()
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
        scores = this["scores"].toMapList().parseScores().orEmpty()
    )

}
