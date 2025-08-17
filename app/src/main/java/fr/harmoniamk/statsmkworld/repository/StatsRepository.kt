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
    var playerOpponentRankList: List<RankingItem>
    var trackRankList: List<RankingItem>
    var playerTrackRankList: List<RankingItem>
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
    private var _playerOpponentRankingList: List<RankingItem> = listOf()
    private var _trackRankList: List<RankingItem> = listOf()
    private var _playerTrackRankList: List<RankingItem> = listOf()

    override var playersRankList: List<RankingItem>
        get() = _playersRankingList
        set(value) { _playersRankingList = value }

    override var opponentRankList: List<RankingItem>
        get() = _opponentRankingList
        set(value) { _opponentRankingList = value }
    override var playerOpponentRankList: List<RankingItem>
        get() = _playerOpponentRankingList
        set(value) { _playerOpponentRankingList = value }
    override var trackRankList: List<RankingItem>
        get() = _trackRankList
        set(value) { _trackRankList = value }
    override var playerTrackRankList: List<RankingItem>
        get() = _playerTrackRankList
        set(value) { _playerTrackRankList = value }

}
