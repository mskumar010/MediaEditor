package com.mediaeditor.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
            AudioEditorScreen(uri = uri, navController = navController)
        }
        
        composable(
            route = Screen.VideoEditor.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true })
        ) { backStack ->
            val uri = backStack.arguments?.getString("uri")?.let { Uri.parse(it) }
            VideoEditorScreen(uri = uri, navController = navController)
        }
        
        composable(Screen.Converter.route) { ConverterScreen(navController) }
        composable(Screen.Batch.route) { BatchScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}
