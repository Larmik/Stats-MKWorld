package fr.harmoniamk.statsmkworld.screen.warList

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

@Composable
fun WarListScreen(
    viewModel: WarListViewModel = hiltViewModel(),
    onWarDetailsClick: (WarDetails) -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val headerBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Colors.green,
                Colors.blue,
                Colors.blue,
                Colors.blue,
                Colors.green
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        )
    }
    BaseScreen(title = stringResource(R.string.all_wars)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.value.wars.forEach { pair ->
                stickyHeader {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().background(headerBrush).border(2.dp, Colors.black)) {
                        MKText(text = pair.first + " (${pair.second.size})", font = Fonts.NunitoBD, fontSize = 16, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
                items(pair.second, key = { it.war.id }) {
                    WarCell(
                        modifier = Modifier.padding(vertical = 5.dp),
                        viewModel = hiltViewModel(
                            key = it.war.id.toString(),
                            creationCallback = { factory: WarCellViewModel.Factory ->
                                factory.create(it)
                            }
                        ),
                        onClick = onWarDetailsClick
                    )
                }
            }
        }
    }

}