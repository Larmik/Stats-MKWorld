package fr.harmoniamk.statsmkworld

import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.sizeOrOne
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests JVM du coeur de calcul du moteur de stats (12 joueurs).
 * Sécurise la non-régression sur A1/A3/A5 (calcul de scores/positions).
 */
class StatsEngineTest {

    private fun position(playerId: String, pos: Int) =
        WarPosition(id = pos.toLong(), playerId = playerId, position = pos)

    /**
     * Une manche 12p où l'équipe hôte prend les positions 1..6.
     * En 12p le modèle ne stocke QUE les positions des 6 joueurs de l'équipe hôte.
     */
    private fun hostTopTrack(index: Int = 0): WarTrack = WarTrack(
        id = index.toLong(),
        index = listOf(index.toString()),
        positions = (1..6).map { position("host$it", it) }
    )

    // --- positionToPoints (barème 12p) ---

    @Test
    fun `positionToPoints 12p bareme connu`() {
        val expected = mapOf(
            1 to 15, 2 to 12, 3 to 10, 4 to 9, 5 to 8, 6 to 7,
            7 to 6, 8 to 5, 9 to 4, 10 to 3, 11 to 2, 12 to 1, 13 to 0
        )
        expected.forEach { (pos, pts) ->
            assertEquals("pos=$pos", pts, pos.positionToPoints(is24p = false))
        }
    }

    @Test
    fun `somme des points d une manche 12p vaut MAX_POINTS_PER_TRACK_12P`() {
        val total = (1..12).sumOf { it.positionToPoints(false) }
        assertEquals(ScoringConstants.MAX_POINTS_PER_TRACK_12P, total)
    }

    // --- WarTrackDetails ---

    @Test
    fun `WarTrackDetails teamScore = somme des points des positions hote`() {
        val details = WarTrackDetails(hostTopTrack(), is24p = false)
        // positions 1..6 => 15+12+10+9+8+7
        assertEquals(61, details.teamScore)
    }

    @Test
    fun `WarTrackDetails displayedResult et displayedDiff coherents`() {
        val details = WarTrackDetails(hostTopTrack(), is24p = false)
        // team=61, opponent = 82-61 = 21, diff = +40
        assertEquals("61 - 21", details.displayedResult)
        assertEquals("+40", details.displayedDiff)
    }

    // --- WarDetails 12p ---

    @Test
    fun `WarDetails scoreHost = somme des teamScore des manches`() {
        val war = War(
            id = 1L,
            teamHost = "host",
            teamOpponent = listOf("opp"),
            tracks = listOf(hostTopTrack(0), hostTopTrack(1)),
            penalties = emptyList(),
            scores = emptyList()
        )
        val details = WarDetails(war)
        assertEquals(122, details.scoreHost)              // 61 x 2
        assertEquals(82 * 2 - 122, details.scoreOpponent) // 42
        assertEquals("122 - 42", details.displayedScore)
        assertEquals("+80", details.displayedDiff)
    }

    @Test
    fun `WarDetails applique les penalites de l hote`() {
        val war = War(
            id = 1L,
            teamHost = "host",
            teamOpponent = listOf("opp"),
            tracks = listOf(hostTopTrack(0)),
            penalties = listOf(WarPenalty(teamId = "host", amount = 10)),
            scores = emptyList()
        )
        val details = WarDetails(war)
        // scoreHost=61, avec penalite -> 51 ; opponent=21
        assertEquals(51, details.scoreHostWithPenalties)
        assertEquals(21, details.scoreOpponentWithPenalties)
        assertEquals("51 - 21", details.displayedScore)
        assertEquals("+30", details.displayedDiff)
    }

    // --- sizeOrOne (A4) ---

    @Test
    fun `sizeOrOne renvoie 1 sur liste vide et la taille sinon`() {
        assertEquals(1, emptyList<Int>().sizeOrOne())
        assertEquals(3, listOf(1, 2, 3).sizeOrOne())
    }
}
