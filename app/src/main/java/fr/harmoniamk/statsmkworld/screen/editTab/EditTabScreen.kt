package fr.harmoniamk.statsmkworld.screen.editTab

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKChip
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import kotlinx.coroutines.launch

@Composable
fun EditTabScreen(viewModel: EditTabViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = viewModel.rows.collectAsStateWithLifecycle()
    // 9 emplacements (max) ; seules les `rows` premières lignes sont affichées (réduire le
    // compteur ne détruit pas la saisie).
    val valuesListName = remember { mutableStateListOf("", "", "", "", "", "", "", "", "") }
    val valuesListScore = remember { mutableStateListOf("", "", "", "", "", "", "", "", "") }

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

    BaseScreen(title = "Tab (PDF)", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // 1. Chips compteur de lignes : − ligne / N lignes / + ligne (min 6, max 9).
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MKChip(
                        label = "− ligne",
                        active = false,
                        enabled = rows.value > 6,
                        onClick = { viewModel.onManageRows(false) }
                    )
                    MKChip(
                        label = "${rows.value} lignes",
                        active = true
                    )
                    MKChip(
                        label = "+ ligne",
                        active = false,
                        enabled = rows.value < 9,
                        onClick = { viewModel.onManageRows(true) }
                    )
                }
            }

            // 2. Lignes de saisie (Adversaire N + Score) générées dynamiquement.
            items(rows.value, key = { it }) { index ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.weight(2f)) {
                        MKTextField(
                            value = valuesListName[index],
                            backgroundColor = Colors.blackAlphaed,
                            onValueChange = { valuesListName[index] = it },
                            placeHolder = "Adversaire ${index + 1}",
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MKTextField(
                            value = valuesListScore[index],
                            backgroundColor = Colors.blackAlphaed,
                            onValueChange = { valuesListScore[index] = it },
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

            // 3. CTA « Tab classique & partager » (MKButton unique, rule 16 / #67).
            item {
                Spacer(Modifier.height(3.dp))
                MKButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.tab_share_cta),
                    icon = R.drawable.ic_share,
                    onClick = {
                        viewModel.generateClassicPdf(
                            players = valuesListName.take(rows.value).filterNot { it.isEmpty() },
                            scores = valuesListScore.take(rows.value).filterNot { it.isEmpty() }
                        )
                    }
                )
            }
        }
    }
}

