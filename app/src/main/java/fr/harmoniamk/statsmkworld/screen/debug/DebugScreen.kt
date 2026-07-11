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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.sendDebugNotification
import fr.harmoniamk.statsmkworld.model.local.MissingPlayer
import fr.harmoniamk.statsmkworld.model.local.OpponentResolution
import fr.harmoniamk.statsmkworld.model.local.UnknownOpponentDiagnostic
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKDialog
import fr.harmoniamk.statsmkworld.ui.MKLoaderDialog
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    val playerId = remember { mutableStateOf("") }
    val matrixMode = viewModel.sharedMatrixMode.collectAsState(false)
    val loading = viewModel.sharedLoading.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val missingPlayers by viewModel.missingPlayers.collectAsState()

    // War en attente de confirmation de suppression (hostRosterId, warId) ; null = pas de dialog.
    var warToDelete by remember { mutableStateOf<Pair<String, Long>?>(null) }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.sharedToast.collect {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sendNotif.collect {
            context.sendDebugNotification("Test notification")
        }
    }

    loading.value?.let {
        MKLoaderDialog(it)
    }

    warToDelete?.let { (hostRosterId, warId) ->
        MKDialog(
            title = "Supprimer la war ?",
            message = "Suppression définitive de la war $warId (hôte $hostRosterId) sur Firebase. Action irréversible.",
            buttonText = "Supprimer",
            secondButtonText = "Annuler",
            onButtonClick = {
                viewModel.onDeleteWar(hostRosterId, warId)
                warToDelete = null
            },
            onSecondButtonClick = { warToDelete = null },
            onDismiss = { warToDelete = null }
        )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onMigrateOpponents() }) {
                    MKText(
                        text = "Migrer les adversaires (teamId → roster)",
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
                        .clickable { viewModel.onDiagnoseUnknownOpponents() }) {
                    MKText(
                        text = "Diagnostiquer les adversaires inconnus",
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
            items(diagnostics, key = { it.warId }) { diagnostic ->
                UnknownOpponentCell(
                    diagnostic = diagnostic,
                    onReattribute = { rawId, newId ->
                        viewModel.onReattributeOpponent(diagnostic.hostRosterId, diagnostic.warId, rawId, newId)
                    },
                    onDelete = { warToDelete = diagnostic.hostRosterId to diagnostic.warId }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onDiagnoseMissingPlayers() }) {
                    MKText(
                        text = "Diagnostiquer les joueurs manquants",
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
            items(missingPlayers, key = { it.playerId }) { missingPlayer ->
                MissingPlayerCell(
                    player = missingPlayer,
                    onAddAsAlly = { viewModel.onAddMissingPlayerAsAlly(missingPlayer.playerId) }
                )
            }
            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.weight(1f).clickable {
                           viewModel.loadWRs()
                        }) {
                            MKText(
                                text = "Test MKWR",
                                font = Fonts.Urbanist,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Colors.blackAlphaed)
                    )
                }
            }

            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.weight(1f).clickable {
                            viewModel.onNotif()
                        }) {
                            MKText(
                                text = "Test notif",
                                font = Fonts.Urbanist,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Colors.blackAlphaed)
                    )
                }
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

@Composable
private fun UnknownOpponentCell(
    diagnostic: UnknownOpponentDiagnostic,
    onReattribute: (rawId: String, newId: String) -> Unit,
    onDelete: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        MKText(
            text = "War ${diagnostic.warId} — ${diagnostic.date} — ${diagnostic.displayedScore}",
            font = Fonts.Urbanist,
            fontSize = 13
        )
        MKText(
            text = "hôte ${diagnostic.teamHost} (nœud ${diagnostic.hostRosterId})",
            fontSize = 11,
            textColor = Colors.blackAlphaed
        )
        diagnostic.unresolvedOpponents.forEach { opponent ->
            Spacer(Modifier.height(6.dp))
            when (val resolution = opponent.resolution) {
                is OpponentResolution.Found -> {
                    MKText(
                        text = "id ${opponent.rawId} → source : ${resolution.teamName} [${resolution.teamTag}] (id ${resolution.teamId})",
                        fontSize = 12
                    )
                    when (resolution.mkworldCandidates.isEmpty()) {
                        true -> MKText(
                            text = "aucun candidat mkworld par nom/tag (irrécupérable en l'état)",
                            fontSize = 11,
                            textColor = Colors.blackAlphaed
                        )
                        else -> {
                            MKText(
                                text = "candidats mkworld (nom/tag) :",
                                fontSize = 11,
                                textColor = Colors.blackAlphaed
                            )
                            resolution.mkworldCandidates.forEach { candidate ->
                                candidate.rosters.forEach { roster ->
                                    Spacer(Modifier.height(4.dp))
                                    MKText(
                                        text = "${candidate.teamName} • ${roster.name} [${roster.tag}] (roster ${roster.rosterId})",
                                        fontSize = 12
                                    )
                                    MKButton(
                                        style = MKButtonStyle.Gradient,
                                        text = "Réattribuer",
                                        onClick = { onReattribute(opponent.rawId, roster.rosterId) }
                                    )
                                }
                            }
                        }
                    }
                }
                OpponentResolution.NotFound -> MKText(
                    text = "id ${opponent.rawId} → introuvable sur MKCentral (irrécupérable)",
                    fontSize = 12
                )
                OpponentResolution.Error -> MKText(
                    text = "id ${opponent.rawId} → erreur de résolution (réseau, réessayer)",
                    fontSize = 12
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        MKButton(
            style = MKButtonStyle.Minor(Colors.black),
            text = "Supprimer la war",
            onClick = onDelete
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Colors.blackAlphaed)
        )
    }
}

@Composable
private fun MissingPlayerCell(
    player: MissingPlayer,
    onAddAsAlly: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        MKText(
            text = "${player.name} [${player.country}]",
            font = Fonts.Urbanist,
            fontSize = 13
        )
        MKText(
            text = "id ${player.playerId} — ${player.warCount} war(s)",
            fontSize = 11,
            textColor = Colors.blackAlphaed
        )
        Spacer(Modifier.height(6.dp))
        MKButton(
            style = MKButtonStyle.Gradient,
            text = "Ajouter en ally",
            onClick = onAddAsAlly
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Colors.blackAlphaed)
        )
    }
}