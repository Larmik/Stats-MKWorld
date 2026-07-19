package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/** Rayon de coin commun aux cartes translucides du pôle Stats (maquette). */
val StatCardRadius = RoundedCornerShape(6.dp)

/**
 * Carte translucide standard du pôle Stats (fond sombre, bordure blanche, radius 6,
 * padding 13) — style « card » de la maquette 5 pôles. Titre en eyebrow optionnel.
 *
 * Extrait de `StatsFullScreen` (rule 16 : mutualisé dès un 2ᵉ écran consommateur — ici
 * les fiches Adversaire/Circuit #27).
 */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, StatCardRadius)
            .border(1.dp, Colors.whiteBorder, StatCardRadius)
            .padding(13.dp)
    ) {
        title?.let {
            when (titleTrailing) {
                null -> Eyebrow(it)
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow(it)
                    Spacer(Modifier.weight(1f))
                    titleTrailing()
                }
            }
            Spacer(Modifier.height(11.dp))
        }
        content()
    }
}

/** Eyebrow (petit titre majuscule blanc) — libellé de section des cartes. */
@Composable
fun Eyebrow(text: String) {
    MKText(
        text = text.uppercase(),
        fontSize = 12,
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        textAlign = TextAlign.Start
    )
}

/**
 * En-tête de fiche : pastille (avatar/logo ou initiales/tag) + nom (Bungee) + sous-titre.
 *
 * - [logo] = URL MKCentral **déjà préfixée** (`https://mkcentral.com…`) → avatar affiché ;
 *   sinon pastille [color]. Les appelants disposant d'un chemin brut (TeamEntity.logo) le
 *   préfixent AVANT de le passer.
 * - [pictureRes] = illustration (circuit) prioritaire sur [logo].
 * - [fallbackText] = texte de la pastille quand ni logo ni picture (initiales joueur / tag).
 */
@Composable
fun StatHeaderCard(
    name: String,
    subtitle: String,
    color: Color,
    logo: String? = null,
    pictureRes: Int? = null,
    fallbackText: String? = null
) {
    StatCard {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                pictureRes != null -> Image(
                    painter = painterResource(pictureRes),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, Colors.white85, RoundedCornerShape(8.dp))
                )
                logo != null -> AsyncImage(
                    model = logo,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
                )
                else -> Box(
                    Modifier.size(52.dp).clip(CircleShape).background(color).border(2.dp, Colors.white85, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (fallbackText) {
                        null -> Image(
                            painter = painterResource(R.drawable.default_logo),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                        )
                        else -> MKText(text = fallbackText, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 16)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                MKText(text = name, font = Fonts.Bungee, textColor = Colors.white, fontSize = 17, textAlign = TextAlign.Start, maxLines = 2)
                MKText(text = subtitle, textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Barre horizontale V/N/D proportionnelle (vert / blanc / rouge) — arrondie, hauteur 13. */
@Composable
fun WinTieLossBar(won: Int, tied: Int, loss: Int) {
    val total = (won + tied + loss).takeIf { it > 0 } ?: 1
    Row(
        Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x38000000))
    ) {
        if (won > 0) Box(Modifier.weight(won.toFloat() / total).fillMaxHeight().background(Colors.green))
        if (tied > 0) Box(Modifier.weight(tied.toFloat() / total).fillMaxHeight().background(Colors.white))
        if (loss > 0) Box(Modifier.weight(loss.toFloat() / total).fillMaxHeight().background(Colors.red))
    }
}

/**
 * Carte « bilan » de la maquette : gros winrate (vert) + résumé V/N/D + barre V/N/D.
 * [subtitle] libre (ex. « de winrate sur N wars/passages »). Utilisée par les fiches
 * Adversaire (« Bilan face à eux ») et Circuit (« Performance »).
 */
@Composable
fun BalanceCard(title: String, winrate: Int, won: Int, tied: Int, loss: Int, subtitle: String) {
    StatCard(title = title) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            MKText(text = "$winrate%", font = Fonts.Urbanist, textColor = winrateColor(winrate), fontSize = 30, textAlign = TextAlign.Start)
            MKText(text = subtitle, textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.End, maxLines = 2)
        }
        Spacer(Modifier.height(6.dp))
        MKText(text = "$won V · $tied N · $loss D", textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(11.dp))
        WinTieLossBar(won, tied, loss)
    }
}

/**
 * Grille régulière de « tuiles » valeur + libellé (fond translucide), [columns] colonnes à
 * poids égal, toutes de même largeur. Utilisée par les fiches (« Séries & scores »,
 * « Scores moyens », « Top 6 / Bot 6 »). [accent] optionnel colore la valeur.
 */
@Composable
fun ColumnScope.StatTiles(tiles: List<StatTile>, columns: Int = 2) {
    tiles.chunked(columns).forEach { rowTiles ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowTiles.forEach { tile ->
                Column(
                    Modifier.weight(1f)
                        .background(Colors.white30, StatCardRadius)
                        .let { base -> tile.borderColor?.let { base.border(1.dp, it.copy(alpha = 0.5f), StatCardRadius) } ?: base }
                        .padding(10.dp)
                ) {
                    MKText(text = tile.value, font = Fonts.Urbanist, textColor = tile.accent ?: Colors.white, fontSize = 18, textAlign = TextAlign.Start, maxLines = 1)
                    MKText(text = tile.label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                }
            }
            repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Tuile valeur + libellé pour [StatTiles] : [accent]/[borderColor] optionnels (Top6/Bot6). */
class StatTile(
    val label: String,
    val value: String,
    val accent: Color? = null,
    val borderColor: Color? = null
)

/**
 * Couleur d'un pourcentage de winrate selon le seuil (mutualisé, fiches Adversaire &
 * Circuit) : **rouge** si < 50 %, **blanc** si = 50 %, **vert** si > 50 %.
 */
fun winrateColor(winrate: Int): Color = when {
    winrate > 50 -> Colors.green
    winrate < 50 -> Colors.red
    else -> Colors.white
}
