package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.extension.warScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.FormStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import java.util.Locale

/**
 * « Forme récente » : vue de référence des stats, comparant 3 fenêtres — all-time,
 * 5 dernières, 10 dernières wars — sur les mêmes indicateurs (une ligne par
 * indicateur, 3 valeurs). Reprend les indicateurs de l'ancienne section historique.
 *
 * Indicateurs (12p) : winrate, score moyen/war, position moyenne (vue joueur) OU
 * score moyen/manche (vue équipe), % de manches gagnées, shocks/war (icône éclair).
 * Deltas des fenêtres récentes vs all-time : flèche + couleur, dont le SENS dépend
 * de l'indicateur (winrate & % maps : plus haut = mieux ; position : plus BAS =
 * mieux → couleur inversée ; shocks : direction ambiguë → neutre, pas de couleur).
 */
@Composable
fun MKRecentFormCell(stats: Stats?) {
    stats?.takeIf { it.warStats.warsPlayed > 0 }?.let { s ->
        // player-based dès que le Stats porte un userId (PlayerStats ET
        // OpponentStats/MapStats en mode individuel) — aligné sur la cellule
        // OpponentRanking. false ⇒ vue équipe (score/moyenne d'équipe inchangés).
        val isPlayer = s.userId != null
        val allTime = s.allTimeForm ?: return@let
        MKExpandableSection(title = stringResource(R.string.recent_form_section)) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                WindowHeader(s.recentForm5, s.recentForm10)

                IndicatorRow(
                    label = stringResource(R.string.form_winrate),
                    info = stringResource(R.string.info_form_winrate),
                    allTime = allTime.winrate.percentValue(),
                    form5 = s.recentForm5?.winrate.percentValue() to metricDelta(s.recentForm5?.winrateDelta, DeltaPolarity.HIGHER_BETTER, "%"),
                    form10 = s.recentForm10?.winrate.percentValue() to metricDelta(s.recentForm10?.winrateDelta, DeltaPolarity.HIGHER_BETTER, "%")
                )
                IndicatorRow(
                    label = stringResource(R.string.form_score),
                    info = stringResource(R.string.info_form_score),
                    allTime = allTime.averageScore.scoreValue(isPlayer),
                    // Score : en vue équipe la valeur affichée est un écart (×2 vs points) → delta doublé.
                    form5 = s.recentForm5?.averageScore.scoreValue(isPlayer) to metricDelta(s.recentForm5?.scoreDelta?.scaleScore(isPlayer), DeltaPolarity.HIGHER_BETTER, ""),
                    form10 = s.recentForm10?.averageScore.scoreValue(isPlayer) to metricDelta(s.recentForm10?.scoreDelta?.scaleScore(isPlayer), DeltaPolarity.HIGHER_BETTER, "")
                )
                when (isPlayer) {
                    true -> IndicatorRow(
                        label = stringResource(R.string.average_position_short),
                        info = stringResource(R.string.info_average_position),
                        allTime = allTime.averagePosition.plainValue(),
                        // Position : plus BAS = mieux → polarité inversée.
                        form5 = s.recentForm5?.averagePosition.plainValue() to metricDelta(s.recentForm5?.positionDelta, DeltaPolarity.LOWER_BETTER, ""),
                        form10 = s.recentForm10?.averagePosition.plainValue() to metricDelta(s.recentForm10?.positionDelta, DeltaPolarity.LOWER_BETTER, "")
                    )
                    else -> IndicatorRow(
                        label = stringResource(R.string.average_map_score_short),
                        info = stringResource(R.string.info_average_map_score),
                        allTime = allTime.averageMapScore.mapScoreValue(),
                        form5 = s.recentForm5?.averageMapScore.mapScoreValue() to metricDelta(s.recentForm5?.mapScoreDelta?.times(2), DeltaPolarity.HIGHER_BETTER, ""),
                        form10 = s.recentForm10?.averageMapScore.mapScoreValue() to metricDelta(s.recentForm10?.mapScoreDelta?.times(2), DeltaPolarity.HIGHER_BETTER, "")
                    )
                }
                IndicatorRow(
                    label = stringResource(R.string.maps_gagn_es),
                    info = stringResource(R.string.info_maps_won),
                    allTime = allTime.mapsWonPercent.percentValue(),
                    form5 = s.recentForm5?.mapsWonPercent.percentValue() to metricDelta(s.recentForm5?.mapsWonDelta, DeltaPolarity.HIGHER_BETTER, "%"),
                    form10 = s.recentForm10?.mapsWonPercent.percentValue() to metricDelta(s.recentForm10?.mapsWonDelta, DeltaPolarity.HIGHER_BETTER, "%")
                )
                ShockIndicatorRow(allTime, s.recentForm5, s.recentForm10)
            }
        }
    }
}

/** En-tête : intitulés des 3 fenêtres (+ petit échantillon signalé si besoin). */
@Composable
private fun WindowHeader(form5: FormStats?, form10: FormStats?) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        MKText(text = "", modifier = Modifier.weight(1.2f))
        WindowTitle(stringResource(R.string.all_time), sample = null)
        WindowTitle(stringResource(R.string.last_n_short, 5), sample = form5.smallSample(5))
        WindowTitle(stringResource(R.string.last_n_short, 10), sample = form10.smallSample(10))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.WindowTitle(title: String, sample: Int?) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        MKText(text = title, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12, textAlign = TextAlign.Center)
        sample?.let {
            MKText(text = stringResource(R.string.small_sample, it), textColor = Colors.grey40, fontSize = 10)
        }
    }
}

/** Une ligne = un indicateur (libellé + bouton info) + 3 valeurs (all-time, 5, 10). */
@Composable
private fun IndicatorRow(
    label: String,
    info: String,
    allTime: String,
    form5: Pair<String, Delta?>,
    form10: Pair<String, Delta?>
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            MKText(text = label, textColor = Colors.white, fontSize = 13, textAlign = TextAlign.Start)
            MKStatInfoButton(title = label, message = info, modifier = Modifier.padding(start = 4.dp))
        }
        ValueCell(allTime, null)
        ValueCell(form5.first, form5.second)
        ValueCell(form10.first, form10.second)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ValueCell(value: String, delta: Delta?) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        MKText(text = value, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, textAlign = TextAlign.Center)
        delta?.let {
            MKText(text = it.text, font = Fonts.NunitoBD, textColor = it.color, fontSize = 11)
        }
    }
}

/** Ligne shocks/war : valeur neutre (direction ambiguë), avec l'icône éclair. */
@Composable
private fun ShockIndicatorRow(allTime: FormStats, form5: FormStats?, form10: FormStats?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.shock), contentDescription = null, modifier = Modifier.size(20.dp))
            MKText(text = stringResource(R.string.shocks_per_war_short), textColor = Colors.white, fontSize = 13, textAlign = TextAlign.Start)
            MKStatInfoButton(
                title = stringResource(R.string.shocks_per_war_short),
                message = stringResource(R.string.info_shocks_per_war),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        ShockValue(allTime.shocksPerWar)
        ShockValue(form5?.shocksPerWar)
        ShockValue(form10?.shocksPerWar)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ShockValue(value: Float?) {
    // Pas de delta coloré : la hausse/baisse de shocks n'est pas clairement « mieux » ou « pire ».
    MKText(
        text = value?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "-",
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        fontSize = 14,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
    )
}

// --- Helpers de formatage / delta -------------------------------------------

private class Delta(val text: String, val color: androidx.compose.ui.graphics.Color)

private enum class DeltaPolarity { HIGHER_BETTER, LOWER_BETTER }

/** Construit un delta affichable (flèche + couleur selon la polarité), null si 0/absent. */
private fun metricDelta(delta: Int?, polarity: DeltaPolarity, suffix: String): Delta? =
    delta?.takeIf { it != 0 }?.let {
        val improved = when (polarity) {
            DeltaPolarity.HIGHER_BETTER -> it > 0
            DeltaPolarity.LOWER_BETTER -> it < 0
        }
        val arrow = if (it > 0) "↗" else "↘"
        Delta("${if (it > 0) "+" else ""}$it$suffix $arrow", if (improved) Colors.green else Colors.red)
    }

private fun Int?.percentValue(): String = this?.let { "$it%" } ?: "-"

private fun Int?.plainValue(): String = this?.toString() ?: "-"

/** Score moyen/war : écart (« +X/-X ») en vue équipe, points bruts en vue joueur. */
private fun Int?.scoreValue(isPlayer: Boolean): String = when {
    this == null -> "-"
    isPlayer -> this.toString()
    else -> this.warScoreToDiff(false)
}

/** Score moyen/manche (vue équipe) : affiché en écart via trackScoreToDiff. */
private fun Int?.mapScoreValue(): String = this?.trackScoreToDiff(false) ?: "-"

/**
 * En vue équipe, la valeur de score/war affichée est un écart (×2 vs les points) :
 * le delta (en points) est doublé pour rester cohérent. Brut en vue joueur.
 */
private fun Int.scaleScore(isPlayer: Boolean): Int = if (isPlayer) this else this * 2

private fun FormStats?.smallSample(requested: Int): Int? =
    this?.takeIf { it.sampleSize < requested }?.sampleSize
