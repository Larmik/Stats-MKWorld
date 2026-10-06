package fr.harmoniamk.statsmkworld.screen.tournament

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKMarkdownText
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.TournamentBadge
import fr.harmoniamk.statsmkworld.ui.stats.StatCard

/**
 * Fiche d'un tournoi officiel (#152), ouverte depuis le badge de la carte score (détail de war, war
 * en cours) : logo, saison, dates, organisateur/mode, lien MKCentral, description et règles
 * (Markdown, traduits sur l'appareil quand c'est possible, original consultable). Graphe racine → pas de bottombar.
 */
@Composable
fun TournamentScreen(viewModel: TournamentViewModel, onBack: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    // Bascule traduction / original : état UI local, conservé à la rotation.
    var showOriginal by rememberSaveable { mutableStateOf(false) }

    BackHandler { onBack() }
    BaseScreen(
        title = state.value.tournament?.let { stringResource(it.label) },
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    ) {
        state.value.tournament?.let { tournament ->
            val details = state.value.details
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                item(key = "header") {
                    StatCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            TournamentBadge(tournament = tournament, height = 52.dp)
                            Column(Modifier.weight(1f)) {
                                MKText(
                                    text = details?.seasonName ?: stringResource(tournament.label),
                                    font = Fonts.Bungee,
                                    textColor = Colors.white,
                                    fontSize = 17,
                                    textAlign = TextAlign.Start,
                                    maxLines = 2
                                )
                                state.value.dateStart?.let { start ->
                                    MKText(
                                        text = stringResource(R.string.tournament_period, start, state.value.dateEnd.orEmpty()),
                                        textColor = Colors.white66,
                                        fontSize = 12,
                                        textAlign = TextAlign.Start
                                    )
                                }
                                listOfNotNull(details?.organizer, details?.mode).takeIf { it.isNotEmpty() }?.let { infos ->
                                    MKText(
                                        text = infos.joinToString(" · "),
                                        textColor = Colors.white66,
                                        fontSize = 12,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }
                when (details) {
                    // Jamais synchronisé (première ouverture hors ligne) : état vide.
                    null -> if (state.value.isLoaded) item(key = "unavailable") {
                        StatCard {
                            MKText(
                                text = stringResource(R.string.tournament_unavailable),
                                textColor = Colors.white,
                                fontSize = 13,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    else -> {
                        state.value.pageUrl?.let { url ->
                            item(key = "mkcentral") {
                                // Bouton seul sur sa ligne : largeur intrinsèque, centré.
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    MKButton(
                                        text = stringResource(R.string.tournament_open_mkcentral),
                                        icon = R.drawable.ic_cup,
                                        onClick = { uriHandler.openUri(url) }
                                    )
                                }
                            }
                        }
                        state.value.descriptionTranslated?.let {
                            item(key = "translation") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    MKText(
                                        text = stringResource(R.string.tournament_auto_translated),
                                        font = Fonts.NunitoIT,
                                        textColor = Colors.white66,
                                        fontSize = 12,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MKButton(
                                        text = stringResource(if (showOriginal) R.string.tournament_show_translation else R.string.tournament_show_original),
                                        onClick = { showOriginal = !showOriginal }
                                    )
                                }
                            }
                        }
                        val description = state.value.descriptionTranslated?.takeUnless { showOriginal } ?: details.description
                        val ruleset = state.value.rulesetTranslated?.takeUnless { showOriginal } ?: state.value.ruleset
                        if (description.isNotBlank()) item(key = "description") {
                            StatCard(title = stringResource(R.string.tournament_description)) {
                                MKMarkdownText(description)
                            }
                        }
                        ruleset?.let {
                            item(key = "ruleset") {
                                StatCard(title = stringResource(R.string.tournament_ruleset)) {
                                    MKMarkdownText(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
