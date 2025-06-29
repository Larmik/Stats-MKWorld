package fr.harmoniamk.statsmkworld.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import fr.harmoniamk.statsmkworld.extension.emit
import fr.harmoniamk.statsmkworld.screen.RootScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val _sharedNotificationsGranted = MutableSharedFlow<Boolean>()

    val sharedNotificationsGranted = _sharedNotificationsGranted.asSharedFlow()

    val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        _sharedNotificationsGranted.emit(granted, lifecycleScope)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()
        splashscreen.setKeepOnScreenCondition { true }
        super.onCreate(savedInstanceState)
        viewModel.processIntent(intent)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.state
            .filterNotNull()
            .onEach {
                it.startDestination?.let { destination ->
                    setContent {
                        RootScreen(startDestination = destination, code = it.code, currentPage = it.currentPage) { finish() }
                    }
                    delay(1000)
                    splashscreen.setKeepOnScreenCondition { false }
                }

            }.launchIn(lifecycleScope)
    }

}