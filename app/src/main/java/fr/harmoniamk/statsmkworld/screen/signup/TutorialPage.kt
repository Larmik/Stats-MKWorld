package fr.harmoniamk.statsmkworld.screen.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.LottieAnimComposable
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun TutorialPage(item: TutorialItem, onClick: (TutorialItem) -> Unit, onCountrySelected: (Country) -> Unit) {

    val country = remember { mutableStateOf<Country?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp)
            .background(color = Colors.black, shape = RoundedCornerShape(15.dp))
            .border(
                width = 2.dp,
                color = Colors.white,
                shape = RoundedCornerShape(15.dp)
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MKText(text = item.title, textColor = Colors.white, font = Fonts.NunitoBD, fontSize = 18)
                    MKText(text = item.text, textColor = Colors.white)
                    if (item == TutorialItem.COUNTRY) {
                        CountrySelector {
                            country.value = it
                        }
                    }
                    Box(contentAlignment = Alignment.Center) {
                        item.image?.let {
                            Image(modifier = Modifier.size(300.dp).padding(vertical = 50.dp), painter = painterResource(it), contentDescription = null)
                        }
                        item.lottie?.let {
                            LottieAnimComposable(modifier = Modifier.heightIn(max = 300.dp), lottieFile = it)
                        }
                    }
                    item.secondText?.let {
                        MKText(text = it, textColor = Colors.white)
                    }

                item.buttonText?.let {
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = it,
                        enabled = item != TutorialItem.COUNTRY || country.value != null,
                        onClick = {
                            if (item == TutorialItem.COUNTRY) country.value?.let(onCountrySelected)
                            onClick(item)
                        }
                    )
                }


            }

    }
}