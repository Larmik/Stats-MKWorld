package fr.harmoniamk.statsmkworld.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

enum class AnimationType(val alignment: Alignment.Vertical) {
    UP_TO_DOWN(Alignment.Top),
    DOWN_TO_UP(Alignment.Bottom)
}

@Composable
fun MKAnimation(visible: Boolean, type: AnimationType, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val expandTransition = remember {
        expandVertically(
            expandFrom = type.alignment,
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }
    val collapseTransition = remember {
        shrinkVertically(
            shrinkTowards = type.alignment,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = expandTransition,
        exit = collapseTransition,
        content = content
    )
}