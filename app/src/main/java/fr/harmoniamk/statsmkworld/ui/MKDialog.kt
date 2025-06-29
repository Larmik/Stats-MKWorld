package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MKDialog(
    title: String,
    message: String,
    buttonText: String,
    secondButtonText: String? = null,
    onButtonClick: () -> Unit = {},
    onSecondButtonClick: () -> Unit = {},
    onDismiss: () -> Unit = {}

) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(
                    color = Colors.white,
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(10.dp)
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(1.dp))
           MKText(
               text = title,
               font = Fonts.Bungee,
               fontSize = 18,
           )
           MKText(text = message)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = buttonText,
                        onClick = onButtonClick
                    )
                secondButtonText?.let {
                    MKButton(
                        style = MKButtonStyle.Minor(Colors.black),
                        text = it,
                        onClick = onSecondButtonClick
                    )
                }
            }
            Spacer(Modifier.height(1.dp))

        }
    }
}