package fr.harmoniamk.statsmkworld.screen.signup

import androidx.annotation.StringRes
import fr.harmoniamk.statsmkworld.R

enum class TutorialItem(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val image: Int? = null,
    val lottie: Int? = null,
    @StringRes val buttonText: Int? = null,
    @StringRes val secondText: Int? = null
) {
    START(
        title = R.string.tuto_1_title,
        text = R.string.tuto_1_text,
        buttonText = R.string.next,
        secondText =R.string.tuto_1_second_text
    ),
    OPEN_APP(
        title = R.string.tuto_2_title,
        text = R.string.tuto_2_text,
        buttonText = R.string.tuto_2_button,
        lottie = R.raw.link,
        secondText = R.string.tuto_2_second_text
    ),
    NOTIFICATIONS(
        title = R.string.tuto_3_title,
        text = R.string.tuto_3_text,
        buttonText = R.string.activer,
        lottie = R.raw.notif,
        secondText = R.string.tuto_3_second_text
    ),

    AUTH(
        title = R.string.tuto_4_title,
        text = R.string.tuto_4_text,
        buttonText = R.string.login,
        lottie = R.raw.discordanim
    ),
    FIND_PLAYER(
        title = R.string.tuto_5_title,
        text = R.string.tuto_5_text,
        image = R.drawable.mkcentralpic,
        lottie = R.raw.search,
    ),
    WELCOME(
        title = R.string.tuto_6_title,
        text = R.string.tuto_6_text,
        lottie = R.raw.finishanim
    ),
    ERROR(
        title = R.string.tuto_7_title,
        text = R.string.tuto_7_message,
        buttonText = R.string.retry,
        secondText = R.string.tuto_7_second_text,
        lottie = R.raw.fail
    )
}