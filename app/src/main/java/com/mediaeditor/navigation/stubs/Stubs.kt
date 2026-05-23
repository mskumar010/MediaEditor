package com.mediaeditor.navigation.stubs

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home Screen (Stub)")
    }
}

@Composable
fun AudioEditorScreen(uri: Uri?, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Audio Editor Screen (Stub)\nURI: ${uri ?: "None"}")
    }
}

@Composable
fun VideoEditorScreen(uri: Uri?, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Video Editor Screen (Stub)\nURI: ${uri ?: "None"}")
    }
}

@Composable
fun ConverterScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Converter Screen (Stub)")
    }
}

@Composable
fun BatchScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Batch Queue Screen (Stub)")
    }
}

@Composable
fun SettingsScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings Screen (Stub)")
    }
}
