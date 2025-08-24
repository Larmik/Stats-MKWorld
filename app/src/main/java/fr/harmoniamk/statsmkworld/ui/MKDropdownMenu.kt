package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.ranking.SortType

@Composable
fun MKDropdownMenu(expanded: Boolean, list: List<Pair<SortType, Boolean>>, onDismiss: () -> Unit, onSelectValue: (SortType) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        list.forEach {
            MKDropdownItem(it, onSelectValue)
        }
    }
}

@Composable
fun MKDropdownItem(pair: Pair<SortType, Boolean>, onSelectValue: (SortType) -> Unit) {
    DropdownMenuItem(
        text = { MKText(text = pair.first.label) },
        trailingIcon = {
            when (pair.second) {
                true -> Image(painter = painterResource(R.drawable.check), contentDescription = null, modifier = Modifier.size(20.dp))
                else -> Spacer(Modifier.size(20.dp))
            }
        },
        onClick = { onSelectValue(pair.first) }
    )

}