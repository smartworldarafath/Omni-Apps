package com.vythera.omniapps

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs    = newBase.getSharedPreferences("omni_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("user_language_code", "en") ?: "en"
        val locale   = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)
        val config   = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Hoist the ViewModel to the top so its init fires immediately,
            // starting loadAll() while the splash is still playing.
            val appViewModel: AppViewModel = viewModel()
            var splashDone      by remember { mutableStateOf(false) }
            var onboardingDone  by remember { mutableStateOf(false) }

            val screen = when {
                !splashDone -> "SPLASH"
                !onboardingDone && appViewModel.state.settings.githubToken.isEmpty() -> "TOKEN_SETUP"
                else -> "HOME"
            }

            Surface(color = Color.Black) {
                Crossfade(
                    targetState   = screen,
                    animationSpec = tween(durationMillis = 500),
                    label         = "nav"
                ) { s ->
                    when (s) {
                        "SPLASH" -> VideoSplashScreen(onFinished = { splashDone = true })
                        "TOKEN_SETUP" -> GitHubTokenOnboarding(
                            onSave = { token ->
                                appViewModel.updateSettings(
                                    appViewModel.state.settings.copy(githubToken = token)
                                )
                                onboardingDone = true
                            },
                            onSkip = { onboardingDone = true }
                        )
                        else -> HomeScreen(viewModel = appViewModel)
                    }
                }
            }
        }
    }
}