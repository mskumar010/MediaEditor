package com.mediaeditor.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mediaeditor.presentation.HomeScreen
import com.mediaeditor.navigation.stubs.*

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        
        composable(
            route = Screen.AudioEditor.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true })
        ) { backStack ->
            val uri = backStack.arguments?.getString("uri")?.let { Uri.parse(it) }
            com.mediaeditor.feature.audioeditor.presentation.AudioEditorScreen(
                uri = uri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.VideoEditor.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true })
        ) { backStack ->
            val uri = backStack.arguments?.getString("uri")?.let { Uri.parse(it) }
            com.mediaeditor.feature.videoeditor.presentation.VideoEditorScreen(
                uri = uri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Converter.route) { 
            com.mediaeditor.feature.converter.presentation.ConverterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Batch.route) { 
            com.mediaeditor.feature.batch.presentation.BatchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Preferences.route) { SettingsScreen(navController) }
    }
}
