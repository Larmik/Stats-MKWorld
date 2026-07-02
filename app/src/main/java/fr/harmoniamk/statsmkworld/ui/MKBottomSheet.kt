package fr.harmoniamk.statsmkworld.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Wrapper maison autour du [ModalBottomSheetLayout] de Material2.
 *
 * Expose trois slots :
 *  - [sheetState] : l'état du sheet piloté par l'appelant ;
 *  - [sheetContent] : le contenu personnalisé affiché dans le sheet ;
 *  - [content] : le corps de l'écran, englobé par le sheet.
 *
 * Le [BackHandler] est géré ici : si le sheet est visible il le ferme,
 * sinon il délègue à [onBack]. Passer [onBack] à `null` désactive ce
 * comportement (l'appelant gère alors lui-même le back).
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
