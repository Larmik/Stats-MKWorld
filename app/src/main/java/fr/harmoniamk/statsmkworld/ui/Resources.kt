package fr.harmoniamk.statsmkworld.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import fr.harmoniamk.statsmkworld.R

object Colors{
    val white = Color(0xFFFFFFFF)
    val whiteAlphaed = Color(0x55FFFFFF)
    val red = Color(0xFFF28B82)
    val blue = Color(0xFFAECBFA)
    val yellow = Color(0xFFFFF176)
    val green = Color(0xFF81C995)
    val purple = Color(0xFFD7AEFB)
    val grey = Color(0xFFF8F9FA)
    val black = Color(0xFF3C4043)
    val blackAlphaed = Color(0x773C4043)
    val transparent = Color(0x00FFFFFF)
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