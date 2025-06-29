package fr.harmoniamk.statsmkworld.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.parsePenalties
import fr.harmoniamk.statsmkworld.extension.parseTracks
import fr.harmoniamk.statsmkworld.extension.toMapList
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
import kotlin.collections.get

interface FirebaseRepositoryInterface{
    //SplashScreen/Login
    fun getUser(id: String): Flow<User?>
    fun getUsers(): Flow<List<User>>
    fun writeUser(user: User): Flow<Unit>

    fun getWars(teamId: String): Flow<List<War>>
    fun writeCurrentWar(war: War): Flow<Unit>
    fun writeWar(war: War): Flow<Unit>
    fun getCurrentWar(teamId: String): Flow<War?>
    fun listenToCurrentWar(teamId: String): Flow<War?>
    fun deleteCurrentWar(teamId: String): Flow<Unit>

    fun getAllies(teamId: String): Flow<List<String>>
    fun writeAlly(teamId: String, ally : String): Flow<Unit>
    fun deleteAlly(teamId: String, ally : String): Flow<Unit>

    fun log(message: String, type: String): Flow<Unit>

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
class FirebaseRepository @Inject constructor(
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : FirebaseRepositoryInterface {

    private val database = Firebase.database.reference

    override fun writeUser(user: User) = flow {
        database.child("users").child(user.id).setValue(user)
        emit(Unit)
    }

    override fun getUser(id: String): Flow<User?>  = callbackFlow {
        database.child("users").child(id.split(".").first()).get().addOnSuccessListener { snapshot ->
            (snapshot.value as? Map<*,*>)?.let { value ->
                launch {
                    val user =  User(
                        id = value["id"].toString(),
                        currentWar = value["currentWar"].toString(),
                        role = value["role"].toString().toIntOrNull() ?: 0,
                    )
                    if (isActive) trySend(user)
                }
            } ?: trySend(null)
        }
        awaitClose {  }
    }.flowOn(Dispatchers.IO)

    override fun writeWar(war: War): Flow<Unit> = dataStoreRepository.mkcTeam
        .mapNotNull { it.id }
        .onEach { database.child("newWars").child(it.toString()).child(war.id.toString()).setValue(war) }
        .mapNotNull {  }



    override fun writeCurrentWar(war: War): Flow<Unit> = flow {
        database.child("currentWars").child(war.teamHost).setValue(war)
        emit(Unit)
    }

    override fun getWars(teamId: String): Flow<List<War>> = callbackFlow {
        database.child("newWars").child(teamId).get().addOnSuccessListener { snapshot ->
            val wars: List<War> = snapshot.children
                .map { it.value as Map<*, *> }
                .map { map -> War(
                    id = map["id"].toString().toLong(),
                    teamHost = map["teamHost"].toString(),
                    teamOpponent = map["teamOpponent"].toString(),
                    tracks = map["tracks"].toMapList().parseTracks().orEmpty(),
                    penalties =  map["penalties"].toMapList().parsePenalties().orEmpty()
                )
                }
            if (isActive) trySend(wars)
        }
        awaitClose {  }
    }.flowOn(Dispatchers.IO)

    override fun getCurrentWar(teamId: String): Flow<War?>  = callbackFlow {
        database.child("currentWars").child(teamId).get().addOnSuccessListener { snapshot ->
            (snapshot.value as? Map<*,*>)?.let { value ->
                launch {
                    val war = War(
                        id = value["id"].toString().toLong(),
                        teamOpponent = value["teamOpponent"].toString(),
                        teamHost = value["teamHost"].toString(),
                        tracks = value["tracks"].toMapList().parseTracks().orEmpty(),
                        penalties =  value["penalties"].toMapList().parsePenalties().orEmpty()
                    )
                    if (isActive) trySend(war)
                }
            } ?: trySend(null)
        }
        awaitClose {  }
    }.flowOn(Dispatchers.IO)



    override fun listenToCurrentWar(teamId: String): Flow<War?> = callbackFlow {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                launch {
                    val war = when (val value = dataSnapshot.child("currentWars").child(teamId).value as? Map<*,*>) {
                        null -> null
                        else -> War(
                            id = value["id"].toString().toLong(),
                            teamOpponent = value["teamOpponent"].toString(),
                            teamHost = value["teamHost"].toString(),
                            tracks = value["tracks"].toMapList().parseTracks().orEmpty(),
                            penalties = listOf()
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

    override fun getAllies(teamId: String): Flow<List<String>> = callbackFlow {
        Log.d("MKDebugOnly", "FirebaseRepository getAllies")
        database.child("allies").child(teamId).get().addOnSuccessListener { snapshot ->
            val teams: List<String> = snapshot.children.map { it.value as String }
            if (isActive) trySend(teams)
        }
        awaitClose {  }
    }.flowOn(Dispatchers.IO)

    override fun getUsers(): Flow<List<User>> = callbackFlow {
        database.child("users").get().addOnSuccessListener { snapshot ->
            val wars: List<User> = snapshot.children
                .map { it.value as Map<*, *> }
                .map { map -> User(
                    id = map["id"].toString(),
                    currentWar = map["currentWar"].toString(),
                    role = map["role"].toString().toIntOrNull() ?: 0,
                )
                }
            if (isActive) trySend(wars)
        }
        awaitClose {  }
    }.flowOn(Dispatchers.IO)

    override fun writeAlly(teamId: String, ally: String): Flow<Unit>  =
        getAllies(teamId)
            .map { database.child("allies").child(teamId).child(it.size.toString()).setValue(ally) }

    override fun deleteAlly(teamId: String, ally: String): Flow<Unit> = flow {
        database.child("allies").child(teamId).child(ally).removeValue()
    }

    override fun log(message: String, type: String): Flow<Unit> = flow {
        database.child("debug").child(Date().displayedString("dd-MM-yyyy")).child(type).child(Date().time.toString()).setValue(message)
    }


}
