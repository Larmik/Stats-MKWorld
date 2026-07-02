package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails

sealed interface WarScoreStyle {
    data object Normal : WarScoreStyle
    data object Small : WarScoreStyle
}

@Composable
fun WarScoreView(
    modifier: Modifier = Modifier,
    style: WarScoreStyle = WarScoreStyle.Normal,
    teamHost: TeamEntity?,
    teamOpponent: List<TeamEntity>?,
    details: WarDetails?,
    rosterName: String? = null,
    rosterId: String? = null
) {

    val teamNameSize: Int
    val logoSize: Dp
    val scoreSize: Int
    val diffSize: Int

    when {
        style is WarScoreStyle.Normal -> {
            teamNameSize = 20
            logoSize = 50.dp
            scoreSize = 32
            diffSize = 24
        }

        else -> {
            teamNameSize = 14
            logoSize = 30.dp
            scoreSize = 24
            diffSize = 18
        }
    }

    val is24p = teamOpponent.orEmpty().size > 1
    val totalShocks = remember(details) {
        details?.warTracks.orEmpty().sumOf { it.track.shocks.orEmpty().sumOf { it.count } }
    }
    val penaltyTotals = remember(details) {
        details?.war?.penalties.orEmpty()
            .groupBy { it.teamId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val sortedWarScores = remember(details) {
        details?.war?.scores?.sortedByDescending { it.score }
    }
    when (is24p) {
        true -> WarScore24pView(
            teamHost = teamHost,
            teamOpponent = teamOpponent,
            details = details,
            rosterId = rosterId,
            is24p = is24p,
            totalShocks = totalShocks,
            penaltyTotals = penaltyTotals
        )

        else -> WarScore12pView(
            modifier = modifier,
            teamHost = teamHost,
            teamOpponent = teamOpponent,
            details = details,
            rosterName = rosterName,
            teamNameSize = teamNameSize,
            logoSize = logoSize,
            scoreSize = scoreSize,
            diffSize = diffSize,
            totalShocks = totalShocks,
            penaltyTotals = penaltyTotals,
            sortedWarScores = sortedWarScores
        )
    }

}

@Composable
private fun WarScore24pView(
    teamHost: TeamEntity?,
    teamOpponent: List<TeamEntity>?,
    details: WarDetails?,
    rosterId: String?,
    is24p: Boolean,
    totalShocks: Int,
    penaltyTotals: Map<String, Int>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sortedScores = remember(details) {
                    details?.scores?.filter { it.score > 0 }?.takeIf { it.size == 4 }?.sortedByDescending { it.score }
                }
                when (val scores = sortedScores) {
                    null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            when (val logo = teamHost?.logo) {
                                null -> Image(
                                    painter = painterResource(R.drawable.default_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp)
                                )

                                else -> AsyncImage(
                                    model = "https://mkcentral.com$logo",
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            MKText(text = when (is24p) {
                                true -> teamHost?.tag
                                else -> teamHost?.name
                            }.orEmpty(), maxLines = 1)
                        }
                        teamOpponent?.forEach { team ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                when (val logo = team.logo) {
                                    null -> Image(
                                        painter = painterResource(R.drawable.default_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp)
                                    )

                                    else -> AsyncImage(
                                        model = "https://mkcentral.com$logo",
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                                MKText(text = when (is24p) {
                                    true -> team.tag
                                    else -> team.name
                                }, maxLines = 1)
                            }
                        }

                    }

                    else -> Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            scores.forEach { score ->
                                val team = teamOpponent?.singleOrNull { it.id == score.teamId } ?: teamHost
                                team?.let {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        when (val logo = it.logo) {
                                            null -> Image(
                                                painter = painterResource(R.drawable.default_logo),
                                                contentDescription = null,
                                                modifier = Modifier.size(50.dp)
                                            )

                                            else -> AsyncImage(
                                                model = "https://mkcentral.com$logo",
                                                contentDescription = null,
                                                modifier = Modifier.size(50.dp)
                                            )
                                        }
                                        MKText(text = when (is24p) {
                                            true -> it.tag
                                            else -> it.name
                                        }, maxLines = 1)
                                        MKText(
                                            text = score.score.toString(),
                                            fontSize = 24,
                                            maxLines = 1,
                                            font = Fonts.NunitoBD
                                        )
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            details?.diffs.orEmpty().forEach {
                                MKText(
                                    modifier = Modifier.weight(3f),
                                    text = it,
                                    textAlign = TextAlign.Center,
                                    font = Fonts.NunitoBD
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            details?.war?.scores?.firstOrNull { it.teamId == rosterId }?.let {
                Spacer(Modifier.height(10.dp))
                MKText(
                    text = it.score.toString(),
                    fontSize = 24,
                    maxLines = 1,
                    font = Fonts.NunitoBD
                )
            }
            totalShocks
                .takeIf { it > 0 }?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.shock),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    MKText(
                        text = "x$it",
                        font = Fonts.NunitoBdIt,
                    )
                }
            }

            details?.war?.penalties.orEmpty().takeIf { it.isNotEmpty() }?.let { penalty ->
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                        .border(1.dp, Colors.white, RoundedCornerShape(5.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MKText(text = "Pénalités", textColor = Colors.white)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            details?.war?.teamHost?.let { teamId ->
                                val total = penaltyTotals[teamId] ?: 0
                                if (total > 0)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp)
                                    ) {

                                        val team = teamOpponent.orEmpty()
                                            .firstOrNull { it.id == teamId } ?: teamHost
                                        when (val logo = team?.logo) {
                                            null -> Image(
                                                painter = painterResource(R.drawable.default_logo),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )

                                            else -> AsyncImage(
                                                model = "https://mkcentral.com$logo",
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(5.dp))
                                        MKText(
                                            text = "-$total",
                                            font = Fonts.NunitoBD,
                                            fontSize = 16,
                                            textColor = Colors.white
                                        )
                                    }
                            }
                            details?.war?.teamOpponent
                                ?.forEach { teamId ->
                                    val total = penaltyTotals[teamId] ?: 0
                                    if (total > 0)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(10.dp)
                                        ) {

                                            val team = teamOpponent.orEmpty()
                                                .firstOrNull { it.id == teamId } ?: teamHost
                                            when (val logo = team?.logo) {
                                                null -> Image(
                                                    painter = painterResource(R.drawable.default_logo),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                else -> AsyncImage(
                                                    model = "https://mkcentral.com$logo",
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(5.dp))
                                            MKText(
                                                text = "-$total",
                                                font = Fonts.NunitoBD,
                                                fontSize = 16,
                                                textColor = Colors.white
                                            )
                                        }
                                }
                        }
                    }
                }
            }

    }
}

@Composable
private fun WarScore12pView(
    modifier: Modifier,
    teamHost: TeamEntity?,
    teamOpponent: List<TeamEntity>?,
    details: WarDetails?,
    rosterName: String?,
    teamNameSize: Int,
    logoSize: Dp,
    scoreSize: Int,
    diffSize: Int,
    totalShocks: Int,
    penaltyTotals: Map<String, Int>,
    sortedWarScores: List<WarScore>?
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val logo = teamHost?.logo) {
                        null -> Image(
                            painter = painterResource(R.drawable.default_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(logoSize)
                                .clip(CircleShape)
                        )

                        else -> AsyncImage(
                            model = "https://mkcentral.com$logo",
                            contentDescription = null,
                            modifier = Modifier
                                .size(logoSize)
                                .clip(CircleShape)
                        )
                    }
                    MKText(text = rosterName.orEmpty(), fontSize = teamNameSize, maxLines = 1)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MKText(
                        text = details?.displayedScore.orEmpty(),
                        fontSize = scoreSize,
                        font = Fonts.NunitoBD,
                        maxLines = 1
                    )
                    MKText(
                        text = details?.displayedDiff.orEmpty(),
                        fontSize = diffSize,
                        font = Fonts.NunitoBD
                    )
                    totalShocks.takeIf { it > 0 }
                        ?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 5.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.shock),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                )
                                MKText(
                                    text = "x$it",
                                    font = Fonts.NunitoBdIt,
                                )
                            }
                        }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    teamOpponent?.forEach {
                        when (val logo = it.logo) {
                            null -> Image(
                                painter = painterResource(R.drawable.default_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(logoSize)
                                    .clip(CircleShape)
                            )

                            else -> AsyncImage(
                                model = "https://mkcentral.com$logo",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(logoSize)
                                    .clip(CircleShape)
                            )
                        }

                        MKText(
                            text = it.name,
                            textColor = Colors.black,
                            fontSize = teamNameSize,
                            maxLines = 1
                        )
                    }

                }
            }
            details?.war?.penalties.orEmpty().takeIf { it.isNotEmpty() }?.let { penalty ->
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                        .border(1.dp, Colors.white, RoundedCornerShape(5.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MKText(text = "Pénalités", textColor = Colors.white)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            sortedWarScores
                                ?.forEach { score ->
                                    val total = penaltyTotals[score.teamId] ?: 0
                                    if (total > 0)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(10.dp)
                                        ) {

                                            val team = teamOpponent.orEmpty()
                                                .firstOrNull { it.id == score.teamId } ?: teamHost
                                            when (val logo = team?.logo) {
                                                null -> Image(
                                                    painter = painterResource(R.drawable.default_logo),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                else -> AsyncImage(
                                                    model = "https://mkcentral.com$logo",
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(5.dp))
                                            MKText(
                                                text = "-$total",
                                                font = Fonts.NunitoBD,
                                                fontSize = 16,
                                                textColor = Colors.white
                                            )
                                        }
                                }
                        }
                    }
                }
            }


    }
}