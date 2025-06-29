package fr.harmoniamk.statsmkworld.screen.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class Country(val countryCode: String, val displayText: String) {
    val rankedCountries =
        listOf("GB", "US", "DE", "FR", "ES", "IT", "MX", "NL", "JP", "BE", "CH", "LX", "CA")
    val rank = when (rankedCountries.contains(countryCode)) {
        true -> 1
        else -> 0
    }
}

fun getCountries(): List<Country> {
    val isoCountryCodes: Array<String> = Locale.getISOCountries()
    val countries: ArrayList<Country> = arrayListOf()

    for (countryCode in isoCountryCodes) {
        val locale = Locale("", countryCode)
        val countryName: String = locale.displayCountry
        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
        val flag = String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        val displayText = "$flag $countryName"
        countries.add(Country(countryCode, displayText))
    }
    return countries.sortedByDescending { it.rank }
}

@Composable
fun CountrySelector(onCountrySelected: (Country) -> Unit) {
    val countries = remember { mutableStateOf<List<Country>>(emptyList()) }
    val selected = remember { mutableStateOf<Country?>(null) }

    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                getCountries()
            }
            countries.value = result
        } catch (e: Exception) {
            countries.value = emptyList()
        }
    }


    LazyColumn(modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 500.dp)) {
        items(countries.value) { country ->
            val backgroundColor = if (country.countryCode == selected.value?.countryCode) {
                Colors.blackAlphaed
            } else {
                Colors.white
            }
            val textColor = if (country.countryCode == selected.value?.countryCode) {
                Colors.white
            } else {
                Colors.black
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = backgroundColor)
                    .clickable {
                        selected.value = country
                        onCountrySelected(country)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MKText(text = country.displayText, textColor = textColor)
            }
        }

    }
}