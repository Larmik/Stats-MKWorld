package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MKSegmentedSelector(items: List<String>, page: Int = 0, onClick: (Int) -> Unit) {

    val currentPage = remember { mutableIntStateOf(page) }

    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Colors.blackAlphaed),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(items.size) { iteration ->
            val text = items[iteration]
            val bgColor = when (iteration == currentPage.intValue) {
                true -> Colors.blackAlphaed
                else -> Colors.transparent
            }
            val textColor = when (iteration == currentPage.intValue) {
                true -> Colors.white
                else -> Colors.black
            }

            Box(
                Modifier
                    .weight(1f)
                    .background(bgColor)
                    .clickable {
                       currentPage.intValue = iteration
                        onClick(iteration)
                    }) {
                MKText(
                    text = text,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 10.dp),
                    font = Fonts.Urbanist,
                    textColor = textColor,
                    fontSize = 16,
                    maxLines = 1
                )
            }

        }
    }
}