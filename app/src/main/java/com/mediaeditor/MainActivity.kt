package com.mediaeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

import androidx.navigation.compose.rememberNavController
import com.mediaeditor.navigation.AppNavHost
import com.mediaeditor.core.ui.theme.HybridTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HybridTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
