package fr.harmoniamk.statsmkworld.screen.editTab

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import kotlinx.coroutines.launch

@Composable
fun EditTabScreen(viewModel: EditTabViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = viewModel.rows.collectAsStateWithLifecycle()
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
    LaunchedEffect(viewModel) {
        launch {
            viewModel.toast.collect {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
        launch {
            viewModel.uri.collect {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Partager l'image"))
            }
        }
    }

    BaseScreen(title = "Tab") {
        MKText(modifier = Modifier.padding(bottom = 10.dp), text = "Génère un tableau de résultats à l'aide des scores des adversaires. Les scores des joueurs et les pénalités sont automatiquement pris en compte. \n \n Il est possible de rajouter jusqu'à trois joueurs supplémentaires. Ne pas oublier d'indiquer le nombre de courses entre parenthèses en cas de war incomplète. \n \n - Tab classique : Score des joueurs et résultat final. \n - Tab détaillé : Tab classique + Circuits, shocks et courbe de progression.")
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        modifier = Modifier.size(40.dp)
                            .clickable(enabled = rows.value > 6, onClick = { viewModel.onManageRows(false) }),
                        painter = painterResource(R.drawable.moins),
                        contentDescription = "Retirer une ligne",
                        colorFilter = ColorFilter.tint(
                            when (rows.value > 6) {
                                true -> Colors.black
                                else -> Colors.blackAlphaed
                            }
                        )
                    )
                    Image(
                        modifier = Modifier.size(40.dp).padding(3.dp)
                            .clickable(enabled = rows.value < 9, onClick = { viewModel.onManageRows(true) }),
                        painter = painterResource(R.drawable.plus),
                        contentDescription = "Ajouter une ligne",
                        colorFilter = ColorFilter.tint(
                            when (rows.value < 9) {
                                true -> Colors.black
                                else -> Colors.blackAlphaed
                            }
                        )
                    )
                }
            }

            items(rows.value, key = { it }) { index ->
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = "Tab classique",
                        onClick = {
                            viewModel.generateClassicPdf(
                                players = valuesListName.toList().filterNot { it.isEmpty() },
                                scores = valuesListScore.toList().filterNot { it.isEmpty() })
                        })
                    /*
                    Spacer(Modifier.width(10.dp))
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = "Tab détaillé",
                        onClick = {
                            viewModel.generateDetailedPdf(
                                players = valuesListName.toList().filterNot { it.isEmpty() },
                                scores = valuesListScore.toList().filterNot { it.isEmpty() })
                        })

                     */
                }
            }
        }
    }
}