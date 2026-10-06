package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarKindFilter

/**
 * Ligne de filtre Amicaux / Officiels (#103), partagée par tous les écrans filtrables.
 * Stateless : le nouveau filtre (dernière case non décochable, cf. [WarKindFilter]) remonte via
 * [onFilterChange] vers le VM, qui recalcule sans re-navigation.
 */
@Composable
fun MKWarKindFilterRow(
    filter: WarKindFilter,
    onFilterChange: (WarKindFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        MKCheckbox(
            label = stringResource(R.string.war_kind_friendly),
            checked = filter.friendly,
            onClick = { onFilterChange(filter.toggleFriendly()) }
        )
        MKCheckbox(
            label = stringResource(R.string.war_kind_official),
            checked = filter.official,
            onClick = { onFilterChange(filter.toggleOfficial()) }
        )
    }
}
