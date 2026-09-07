package fr.harmoniamk.statsmkworld.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Wrapper maison autour du [ModalBottomSheetLayout] Material2. Le back ferme le sheet s'il
 * est visible, sinon délègue à [onBack] (`null` = l'appelant gère le back).
 */
@Composable
fun MKBottomSheet(
    sheetState: ModalBottomSheetState,
    sheetContent: @Composable () -> Unit,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    onBack?.let { back ->
        BackHandler {
            when (sheetState.isVisible) {
                true -> scope.launch { sheetState.hide() }
                else -> back()
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = { sheetContent() },
        content = content
    )
}
