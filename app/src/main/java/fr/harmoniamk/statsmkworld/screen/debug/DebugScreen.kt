package fr.harmoniamk.statsmkworld.screen.debug

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKLoaderDialog
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    val playerId = remember { mutableStateOf("") }
    val matrixMode = viewModel.sharedMatrixMode.collectAsState(false)
    val loading = viewModel.sharedLoading.collectAsState()

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.sharedToast.collect {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    loading.value?.let {
        MKLoaderDialog(it)
    }

    BaseScreen(title = "Debug") {
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onUpdateTags() }) {
                    MKText(
                        text = "Mettre à jour les tags",
                        font = Fonts.Urbanist,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Colors.blackAlphaed)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onUpdateBotData() }) {
                    MKText(
                        text = "Mettre à jour les données LariisBot",
                        font = Fonts.Urbanist,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Colors.blackAlphaed)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onManageTransferts() }) {
                    MKText(
                        text = "Mettre à jour les transferts",
                        font = Fonts.Urbanist,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Colors.blackAlphaed)
                )
            }
            item {
                Column {

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.weight(1f).clickable {
                            when (matrixMode.value) {
                                true -> viewModel.onMatrixEnd()
                                else -> viewModel.onMatrix(playerId.value)
                            }
                        }) {
                            MKText(
                                text = when (matrixMode.value) {
                                    true -> "Sortir de la matrice"
                                    else -> "Entrer dans la matrice"
                                },
                                font = Fonts.Urbanist,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                        if (!matrixMode.value)
                            MKTextField(
                                modifier = Modifier.width(100.dp),
                                value = playerId.value,
                                backgroundColor = Colors.blackAlphaed,
                                onValueChange = {
                                    playerId.value = it
                                },
                                placeHolderRes = R.string.id_joueur
                            )
                    }
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Colors.blackAlphaed)
                    )
                }
            }
        }
    }
}