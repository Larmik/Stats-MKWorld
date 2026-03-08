package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MKSelectorViewPager(state: PagerState, list: List<String>, content: @Composable ColumnScope.() -> Unit) {
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Colors.blackAlphaed),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(list.count()) { iteration ->
            val text = list[iteration]
            val bgColor = when (iteration == state.currentPage) {
                true -> Colors.blackAlphaed
                else -> Colors.transparent
            }
            val textColor = when (iteration == state.currentPage) {
                true -> Colors.white
                else -> Colors.black
            }

            Box(Modifier
                .weight(1f)
                .background(bgColor)
                .clickable {
                    scope.launch {
                        state.animateScrollToPage(iteration)
                    }
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
    HorizontalPager(
        beyondViewportPageCount = 2,
        state = state
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}