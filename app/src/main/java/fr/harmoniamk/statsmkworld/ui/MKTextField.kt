package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MKTextField(
    modifier: Modifier = Modifier.fillMaxWidth(),
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeHolderRes: Int,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions(),
    backgroundColor: Color = Colors.transparent,
    errorMessage: String? = null
) {

    val borderColor = when (errorMessage.isNullOrEmpty()) {
        true -> Colors.white
        else -> Colors.red
    }
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.colors(
        unfocusedTextColor = Colors.white,
        focusedTextColor = Colors.white,
        disabledTextColor = Colors.white,
        unfocusedContainerColor = backgroundColor,
        focusedContainerColor = backgroundColor,
        disabledContainerColor = backgroundColor,

    )
    Column {
        BasicTextField(
            value = value,
            modifier = modifier
                .padding(vertical = 5.dp)
                .border(2.dp, borderColor)
                .background(color = Colors.transparent, shape = RoundedCornerShape(5.dp))
                .indicatorLine(true, false, interactionSource, colors)
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = 50.dp
                ),
            onValueChange = onValueChange,
            enabled = true,
            readOnly = false,
            textStyle = TextStyle(
                color = Colors.white,
                fontFamily = FontFamily(Fonts.NunitoRG),
                fontSize = TextUnit(14f, TextUnitType.Sp)
            ),
            cursorBrush = SolidColor(Colors.white),
            visualTransformation = when (keyboardType) {
                KeyboardType.Password -> PasswordVisualTransformation()
                else -> VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            keyboardActions =  keyboardActions,
            interactionSource = interactionSource,
            singleLine = true,
            maxLines = 1,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value,
                    visualTransformation = VisualTransformation.None,
                    innerTextField = innerTextField,
                    placeholder = {
                        MKText(
                            text = stringResource(id = placeHolderRes),
                            textColor = Colors.grey,
                            fontSize = 13
                        )
                    },
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = true,
                    enabled = true,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                )
            }
        )
        MKAnimation(visible = !errorMessage.isNullOrEmpty(), type = AnimationType.UP_TO_DOWN) {
            MKText(text = errorMessage.orEmpty(), textColor = Colors.red, fontSize = 12)
        }
    }

}

@Preview
@Composable
@ExperimentalMaterial3Api
fun MKTextFieldPreview() {
    MKTextField(value = "", placeHolderRes = R.string.app_name, onValueChange = {})
}
