package fr.harmoniamk.statsmkworld.datasource.local

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.MKDatabase
import fr.harmoniamk.statsmkworld.database.entities.TournamentEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TournamentLocalDataSourceInterface {
    fun getTournaments(): Flow<List<TournamentEntity>>
    fun getTournament(id: String): Flow<TournamentEntity?>
    suspend fun upsert(tournament: TournamentEntity)
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface TournamentLocalDataSourceModule {
    @Binds
    @Singleton
    fun bind(impl: TournamentLocalDataSource): TournamentLocalDataSourceInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class TournamentLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) : TournamentLocalDataSourceInterface {

    private val dao = MKDatabase.getInstance(context).tournamentDao()

    override fun getTournaments(): Flow<List<TournamentEntity>> = dao.getTournaments()

    override fun getTournament(id: String): Flow<TournamentEntity?> = dao.getTournament(id)

    override suspend fun upsert(tournament: TournamentEntity) = dao.upsert(tournament)

}
