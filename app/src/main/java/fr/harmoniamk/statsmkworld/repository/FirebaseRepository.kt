package fr.harmoniamk.statsmkworld.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface FirebaseRepositoryInterface {
    //SplashScreen/Login
    fun getUsers(teamId: String): Flow<List<User>>
    fun getUser(teamId: String, id: String): Flow<User?>
    fun writeUser(teamId: String, user: User): Flow<Unit>
    fun deleteUser(teamId: String, id: String): Flow<Unit>

    fun getWars(teamId: String): Flow<List<War>>
    fun writeWar(war: War): Flow<Unit>

    fun getCurrentWar(teamId: String): Flow<War?>
    fun listenToCurrentWar(teamId: String): Flow<War?>
    fun writeCurrentWar(war: War): Flow<Unit>
    fun deleteCurrentWar(teamId: String): Flow<Unit>

    fun getAllies(teamId: String): Flow<List<User>>
    fun writeAlly(teamId: String, user: User): Flow<Unit>
    fun deleteAlly(teamId: String, ally: String): Flow<Unit>

    fun log(message: String, type: String): Flow<Unit>
    fun writeTags(tags: List<Tag>) : Flow<Unit>


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

    override fun writeUser(teamId: String, user: User) = flow {
        database.child("users").child(teamId).child(user.id).setValue(user)
        emit(Unit)
    }

    override fun deleteUser(
        teamId: String,
        id: String
    ): Flow<Unit> = flow {
        database.child("users").child(teamId).child(id).removeValue()
        emit(Unit)
    }
    override fun getUser(teamId: String, id: String): Flow<User?> = callbackFlow {
        database.child("users").child(teamId).child(id).get().addOnSuccessListener { snapshot ->
            (snapshot.value as? Map<*, *>)?.let { value ->
                launch {
                    val user = User(
                        id = value["id"].toString(),
                        currentWar = value["currentWar"].toString(),
                        role = value["role"].toString().toIntOrNull() ?: 0,
                        name = value["name"].toString(),
                        discordId = value["discordId"].toString()
                    )
                    if (isActive) trySend(user)
                }
            } ?: trySend(null)
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun writeWar(war: War): Flow<Unit> = dataStoreRepository.mkcPlayer
        .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
        .onEach { database.child("wars").child(it).child(war.id.toString()).setValue(war) }
        .map { }

    override fun writeCurrentWar(war: War): Flow<Unit> = dataStoreRepository.mkcPlayer
        .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
        .onEach { database.child("currentWars").child(it).setValue(war) }
        .map { }

    override fun getWars(teamId: String): Flow<List<War>> = callbackFlow {
        database.child("wars").child(teamId).get().addOnSuccessListener { snapshot ->
            val wars: List<War> = snapshot.children
                .map { it.value as Map<*, *> }
                .map { map ->
                    War(
                        id = map["id"].toString().toLong(),
                        teamHost = map["teamHost"].toString(),
                        teamOpponent = map["teamOpponent"] as List<String>,
                        tracks = map["tracks"].toMapList().parseTracks().orEmpty(),
                        penalties = map["penalties"].toMapList().parsePenalties().orEmpty(),
                        scores = map["scores"].toMapList().parseScores().orEmpty()
                    )
                }
            if (isActive) trySend(wars)
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun getCurrentWar(teamId: String): Flow<War?> = callbackFlow {
        database.child("currentWars").child(teamId).get().addOnSuccessListener { snapshot ->
            (snapshot.value as? Map<*, *>)?.let { value ->
                launch {
                    val war = War(
                        id = value["id"].toString().toLong(),
                        teamOpponent = value["teamOpponent"] as List<String>,
                        teamHost = value["teamHost"].toString(),
                        tracks = value["tracks"].toMapList().parseTracks().orEmpty(),
                        penalties = value["penalties"].toMapList().parsePenalties().orEmpty(),
                        scores = value["scores"].toMapList().parseScores().orEmpty()
                    )
                    if (isActive) trySend(war)
                }
            } ?: trySend(null)
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun listenToCurrentWar(teamId: String): Flow<War?> = callbackFlow {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                launch {
                    val war = when (val value =
                        dataSnapshot.child("currentWars").child(teamId).value as? Map<*, *>) {
                        null -> null
                        else -> War(
                            id = value["id"].toString().toLong(),
                            teamOpponent = value["teamOpponent"] as List<String>,
                            teamHost = value["teamHost"].toString(),
                            tracks = value["tracks"].toMapList().parseTracks().orEmpty(),
                            penalties = value["penalties"].toMapList().parsePenalties().orEmpty(),
                            scores = value["scores"].toMapList().parseScores().orEmpty()
                        )
                    }
                    if (isActive) trySend(war)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        database.addValueEventListener(postListener)
        awaitClose { database.removeEventListener(postListener) }
    }.flowOn(Dispatchers.IO)

    override fun deleteCurrentWar(teamId: String) = flow {
        database.child("currentWars").child(teamId).removeValue()
        emit(Unit)
    }


    override fun writeAlly(
        teamId: String,
        user: User
    ): Flow<Unit> = flow {
        database.child("newAllies").child(teamId).child(user.id).setValue(user)
        emit(Unit)
    }

    override fun getUsers(teamId: String): Flow<List<User>> = callbackFlow {
        database.child("users").child(teamId).get().addOnSuccessListener { snapshot ->
            val wars: List<User> = snapshot.children
                .map { it.value as Map<*, *> }
                .map { map ->
                    User(
                        id = map["id"].toString(),
                        currentWar = map["currentWar"].toString(),
                        role = map["role"].toString().toIntOrNull() ?: 0,
                        name = map["name"].toString(),
                        discordId = map["discordId"].toString()
                    )
                }
            if (isActive) trySend(wars)
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun deleteAlly(teamId: String, ally: String): Flow<Unit> = flow {
        database.child("newAllies").child(teamId).child(ally).removeValue()
    }

    override fun getAllies(teamId: String): Flow<List<User>> = callbackFlow {
        database.child("newAllies").child(teamId).get().addOnSuccessListener { snapshot ->
            val wars: List<User> = snapshot.children
                .map { it.value as Map<*, *> }
                .map { map ->
                    User(
                        id = map["id"].toString(),
                        currentWar = map["currentWar"].toString(),
                        role = map["role"].toString().toIntOrNull() ?: 0,
                        name = map["name"].toString(),
                        discordId = map["discordId"].toString()
                    )
                }
            if (isActive) trySend(wars)
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun log(message: String, type: String): Flow<Unit> = flow {
        database.child("debug").child(Date().displayedString("dd-MM-yyyy")).child(type)
            .child(Date().time.toString()).setValue(message)
    }

    override fun writeTags(tags: List<Tag>): Flow<Unit> = flow {
        database.child("tags").setValue(tags)
        emit(Unit)
    }


}
