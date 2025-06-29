package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun BaseScreen(modifier: Modifier = Modifier, title: String? = null, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = listOf(
                    Colors.black,
                    Colors.blue,
                    Colors.green,
                    Colors.yellow,
                    Colors.red,
                    Colors.purple,
                    Colors.black
                ),
                start = Offset(0f, 0f),
                end = Offset.Infinite
            ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            title?.let {
                MKText(text = it, textColor = Colors.black, fontSize = 24, font = Fonts.Bungee)
                subtitle?.let { sub ->
                    MKText(text = sub, textColor = Colors.black, fontSize = 18, font = Fonts.NunitoBD)

                }
                Spacer(Modifier.height(20.dp))
            }
            content()
        }
    }
}
