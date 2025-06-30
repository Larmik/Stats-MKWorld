package fr.harmoniamk.statsmkworld.screen.signup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.activity.MainActivity
import fr.harmoniamk.statsmkworld.extension.getActivity
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    currentPage: Int?,
    onBack: () -> Unit,
    onNext: () -> Unit
) {

    val context = LocalContext.current
    val activity = (context.getActivity() as MainActivity)

    val scope = rememberCoroutineScope()
    val selectedIndex = remember { mutableIntStateOf(currentPage ?: 0) }
    val pagerState = rememberPagerState()
    val state = viewModel.state.collectAsState()

    LaunchedEffect(state.value) {
        state.value.currentPage?.let { page ->
            selectedIndex.intValue = page
            scope.launch { pagerState.animateScrollToPage(selectedIndex.intValue) }
        }

    }

    LaunchedEffect(Unit) {
         viewModel.onNext.collect {
             onNext()
         }
    }


    LaunchedEffect(Unit) {
        viewModel.showNotif.collect {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
               activity.notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
           }
        }
    }
    LaunchedEffect(activity.sharedNotificationsGranted) {
        activity.sharedNotificationsGranted.collect {
            selectedIndex.intValue += 1
            scope.launch { pagerState.animateScrollToPage(selectedIndex.intValue) }
        }
    }

    BackHandler { onBack() }

    BaseScreen(title = stringResource(R.string.welcome)) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                modifier = Modifier.fillMaxWidth(),
                count = TutorialItem.entries.size,
                state = pagerState,
                userScrollEnabled = false
            ) { pagerScope ->
                TutorialPage(TutorialItem.entries[pagerScope],
                    onClick = {
                        when (it) {
                            TutorialItem.AUTH -> {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://discord.com/oauth2/authorize?client_id=1371774122388099195&response_type=code&redirect_uri=https%3A%2F%2Fstatsmkworld.com&scope=identify+guilds+email+guilds.members.read+openid".toUri()
                                )
                                context.startActivity(intent)
                            }
                            TutorialItem.NOTIFICATIONS -> viewModel.requestNotifications()
                            TutorialItem.OPEN_APP -> {
                               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                     val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                     context.startActivity(intent)
                               }
                                selectedIndex.intValue += 1
                                scope.launch {
                                    delay(500)
                                    pagerState.animateScrollToPage(selectedIndex.intValue)
                                }
                            }
                            TutorialItem.ERROR -> viewModel.onRetry()
                            else -> {
                                selectedIndex.intValue += 1
                                scope.launch { pagerState.animateScrollToPage(selectedIndex.intValue) }
                            }
                        }
                    })
            }
        }

    }
}
