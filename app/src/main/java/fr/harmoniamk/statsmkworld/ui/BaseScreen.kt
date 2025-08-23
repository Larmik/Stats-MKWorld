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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BaseViewModel @Inject constructor(dataStoreRepository: DataStoreRepositoryInterface): ViewModel() {
    val colors = dataStoreRepository.matrixMode
        .map {
            when (it) {
                true -> listOf(
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

@Composable
fun BaseScreen(modifier: Modifier = Modifier, title: String? = null, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {

    val viewModel: BaseViewModel = hiltViewModel()
    val colors = viewModel.colors.collectAsState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = colors.value,
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
