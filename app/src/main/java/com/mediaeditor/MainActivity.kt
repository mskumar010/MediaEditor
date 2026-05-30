package com.mediaeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.mediaeditor.core.ui.theme.HybridTheme
import com.mediaeditor.feature.settings.domain.UserPreferencesRepository
import com.mediaeditor.feature.settings.domain.model.ThemeMode
import com.mediaeditor.feature.settings.domain.model.UserPreferences
import com.mediaeditor.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val userPrefs by preferencesRepository.userPreferences.collectAsState(initial = UserPreferences())
            
            val darkTheme = when (userPrefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            HybridTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
