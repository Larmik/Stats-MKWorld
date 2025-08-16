package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import javax.inject.Inject
import javax.inject.Singleton

interface StatsRepositoryInterface {
    var playersRankList: List<RankingItem>
    var opponentRankList: List<RankingItem>
    var trackRankList: List<RankingItem>
}


@Module
@InstallIn(SingletonComponent::class)
interface StatsRepositoryModule {
    @Singleton
    @Binds
    fun bind(impl: StatsRepository): StatsRepositoryInterface
}

class StatsRepository @Inject constructor() : StatsRepositoryInterface {

    private var _playersRankingList: List<RankingItem> = listOf()
    private var _opponentRankingList: List<RankingItem> = listOf()
    private var _trackRankList: List<RankingItem> = listOf()

    override var playersRankList: List<RankingItem>
        get() = _playersRankingList
        set(value) { _playersRankingList = value }

    override var opponentRankList: List<RankingItem>
        get() = _opponentRankingList
        set(value) { _opponentRankingList = value }
    override var trackRankList: List<RankingItem>
        get() = _trackRankList
        set(value) { _trackRankList = value }

}
