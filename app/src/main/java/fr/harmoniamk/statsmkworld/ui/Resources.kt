package fr.harmoniamk.statsmkworld.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import fr.harmoniamk.statsmkworld.R

object Colors{
    val white = Color(0xFFFFFFFF)
    val whiteAlphaed = Color(0x55FFFFFF)
    // Rouge/vert défaite/victoire, assombris vs les pastels maquette (--loss/--win) : servent
    // aussi de TEXTE sur le fond clair du dégradé, où les pastels manquaient de contraste (#50 pt.6).
    val red = Color(0xFFE05D51)
    val blue = Color(0xFFAECBFA)
    val yellow = Color(0xFFFFF176)
    val green = Color(0xFF4FA96C)
    val purple = Color(0xFFD7AEFB)
    val gold = Color(0xFFD4AF37) // --gold de la maquette (pastille de rôle Leader)
    val grey = Color(0xFFF8F9FA)
    val black = Color(0xFF3C4043)
    val blackAlphaed = Color(0x773C4043)
    val transparent = Color(0x00FFFFFF)
    // Bande d'appbar (`.appbar` rgba(48,51,54,.5), #50 pt.2).
    val appbar = Color(0x80303336)
    // Blancs translucides (bordures / libellés secondaires des cartes sombres du dashboard).
    val whiteBorder = Color(0xEBFFFFFF) // rgba(255,255,255,.92) — bordure de carte
    val whiteBorderSoft = Color(0x73FFFFFF) // rgba(255,255,255,.45) — bordure douce (segmented)
    val white30 = Color(0x4DFFFFFF) // rgba(255,255,255,.30) — fond translucide (tuiles/segmented)
    val white85 = Color(0xD9FFFFFF) // rgba(255,255,255,.85) — bordure de pastille
    val white70 = Color(0xB3FFFFFF)
    val white66 = Color(0xA8FFFFFF)
    val white55 = Color(0x8CFFFFFF)
    val grey10 = Color(0xFFF5F5F5)
    val grey20 = Color(0xFFEEEEEE)
    val grey30 = Color(0xFFE0E0E0)
    val grey40 = Color(0xFFBDBDBD)
    val grey50 = Color(0xFF9E9E9E)
    val grey60 = Color(0xFF757575)
    val grey70 = Color(0xFF616161)
    val grey80 = Color(0xFF424242)
    val grey90 = Color(0xFF212121)
}

object Fonts {
    val Bungee
        get() = Font(
            resId = R.font.bungee,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )
    val NunitoRG
        get() = Font(
            resId = R.font.nunito_rg,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )
    val NunitoBD
        get() = Font(
            resId = R.font.nunito_bd,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )
    val NunitoIT
        get() = Font(
            resId = R.font.nunito_it,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )
    val NunitoBdIt
        get() = Font(
            resId = R.font.nunito_bd_it,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )

    val Urbanist
        get() = Font(
            resId = R.font.urbanist,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,

        )

    val MKPosition
        get() = Font(
            resId = R.font.mkworld,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,

        )

}