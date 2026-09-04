package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.BuildConfig
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BaseViewModel @Inject constructor(dataStoreRepository: DataStoreRepositoryInterface): ViewModel() {
    val colors = dataStoreRepository.matrixMode
        .map {
            when  {
                it -> listOf(
                    Colors.grey90,
                    Colors.grey70,
                    Colors.grey50,
                    Colors.grey30,
                    Colors.grey10,
                    Colors.grey30,
                    Colors.grey50,
                    Colors.grey70,
                    Colors.grey90
                )
                BuildConfig.DEBUG -> listOf(
                    Colors.black,
                    Colors.purple,
                    Colors.red,
                    Colors.yellow,
                    Colors.green,
                    Colors.blue,
                    Colors.black
                )
                else -> listOf(
                    Colors.black,
                    Colors.blue,
                    Colors.green,
                    Colors.yellow,
                    Colors.red,
                    Colors.purple,
                    Colors.black
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),  listOf(
            Colors.black,
            Colors.blue,
            Colors.green,
            Colors.yellow,
            Colors.red,
            Colors.purple,
            Colors.black
        ))
}

/**
 * Bouton d'action circulaire-arrondi de l'appbar (`.ic-btn` de la maquette : carré
 * 32 dp, coins 10 dp, fond blanc translucide, bordure douce, icône blanche).
 * Utilisé pour le retour ([BaseScreen] `onBack`) et la loupe/registre (`onSearch`).
 */
@Composable
private fun AppBarIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Colors.white30, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.whiteBorderSoft, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Colors.white,
            modifier = Modifier.size(17.dp)
        )
    }
}

/**
 * Écran de base : fond en dégradé + **appbar conforme maquette** (`.appbar`, #50 pt.2) :
 * bouton retour optionnel ([onBack]) à gauche, titre (Bungee) + sous-titre alignés à
 * gauche, action loupe→registre optionnelle ([onSearch]) à droite. Le titre est en blanc,
 * comme dans la maquette.
 */
@Composable
fun BaseScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    // Action droite de l'appbar : icône/libellé personnalisables (défaut = loupe→registre).
    // Ex. écran Wars : « Créer une war » (ic_add), affiché seulement si aucune war en cours (#50).
    actionIcon: Int = R.drawable.ic_search,
    actionContentDescription: String? = null,
    // Contenu additionnel aligné à DROITE dans l'app bar, avant l'icône d'action (#70) :
    // sert au menu déroulant de saison (MKSeasonDropdown), mutualisé sur Accueil/Wars/
    // Stats/Classements. Null = rien (comportement inchangé pour les autres écrans).
    headerTrailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    val viewModel: BaseViewModel = hiltViewModel()
    val colors = viewModel.colors.collectAsState()
    // Inset de la status bar : la bande d'appbar (et le fond) s'étend DERRIÈRE la status bar
    // (edge-to-edge, cf. MainActivity.enableEdgeToEdge), mais le CONTENU (titre/boutons) est
    // décalé sous elle pour ne pas passer dessous (#50 header edge-to-edge).
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = colors.value,
                start = Offset(0f, 0f),
                end = Offset.Infinite
            ))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            title?.let {
                // Bande d'appbar pleine largeur ET jusqu'au bord haut physique (`.appbar` :
                // fond rgba(48,51,54,.5)) : le fond couvre la zone status bar, le contenu de
                // la Row est repoussé dessous par le top padding = inset status bar.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Colors.appbar)
                        .padding(top = statusBarTop)
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    onBack?.let { back ->
                        AppBarIconButton(
                            iconRes = R.drawable.ic_arrow_back,
                            contentDescription = stringResource(R.string.back),
                            onClick = back
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        MKText(text = it, textColor = Colors.white, fontSize = 22, font = Fonts.Bungee, textAlign = TextAlign.Start)
                        subtitle?.let { sub ->
                            MKText(text = sub, textColor = Colors.white66, fontSize = 14, font = Fonts.NunitoBD, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    // Dropdown de saison (#70) aligné à droite, avant l'icône d'action.
                    headerTrailing?.invoke()
                    onSearch?.let { action ->
                        AppBarIconButton(
                            iconRes = actionIcon,
                            contentDescription = actionContentDescription ?: stringResource(R.string.rechercher),
                            onClick = action
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Marge latérale du contenu (l'appbar est pleine largeur, elle, hors padding).
                    .padding(horizontal = 16.dp)
                    // Sans appbar (pas de titre), compenser soi-même l'inset status bar + marge d'origine.
                    .padding(top = if (title == null) statusBarTop + 16.dp else 0.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}
