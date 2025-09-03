package fr.harmoniamk.statsmkworld.screen.editTab

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKTextField

@Composable
fun EditTabScreen(viewModel: EditTabViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = viewModel.rows.collectAsState()
    val valuesListName = remember {
        mutableStateListOf(
            "", "", "", "", "", "", "", "", ""
        )
    }
    val valuesListScore = remember {
        mutableStateListOf(
            "", "", "", "", "", "", "", "", ""
        )
    }

    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        viewModel.toast.collect {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.uri.collect {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager l'image"))
        }
    }

    BaseScreen(title = "Tab") {
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            items(rows.value) { index ->
                Row {
                    Box(Modifier.weight(2f)) {
                        MKTextField(
                            value = valuesListName[index],
                            backgroundColor = Colors.blackAlphaed,
                            onValueChange = {
                                valuesListName[index] = it
                            },
                            placeHolder = "Nom adversaire ${index + 1}",
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MKTextField(
                            value = valuesListScore[index],
                            backgroundColor = Colors.blackAlphaed,
                            onValueChange = {
                                valuesListScore[index] = it
                            },
                            placeHolder = "Score",
                            keyboardType = KeyboardType.Number,
                            imeAction = when (index == rows.value - 1) {
                                true -> ImeAction.Done
                                else -> ImeAction.Next
                            }
                        )
                    }
                }
            }
            item {
                Row {
                    MKButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                        style = MKButtonStyle.Minor(Colors.black),
                        text = "Supprimer une ligne",
                        enabled = rows.value > 6,
                        onClick = { viewModel.onManageRows(false) })
                    MKButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                        style = MKButtonStyle.Minor(Colors.black),
                        text = "Ajouter une ligne",
                        enabled = rows.value < 9,
                        onClick = { viewModel.onManageRows(true) })
                }
            }

            item {
                Row {
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = "Tab classique",
                        onClick = {
                            viewModel.generateClassicPdf(
                                players = valuesListName.toList().filterNot { it.isEmpty() },
                                scores = valuesListScore.toList().filterNot { it.isEmpty() })
                        })
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = "Tab détaillé",
                        onClick = {
                            viewModel.generateDetailedPdf(
                                players = valuesListName.toList().filterNot { it.isEmpty() },
                                scores = valuesListScore.toList().filterNot { it.isEmpty() })
                        })
                }
            }
        }
    }
}