package fr.harmoniamk.statsmkworld.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LottieAnimComposable(
    modifier: Modifier = Modifier,
    lottieFile: Int,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieFile))
    val lottieAnimation by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = isPlaying,
        speed = 1f,
        restartOnPlay = false
    )

    if (lottieAnimation == 1f) onDismiss()

    return LottieAnimation(
        composition = composition,
        progress = lottieAnimation,
        modifier = modifier
    )
}
