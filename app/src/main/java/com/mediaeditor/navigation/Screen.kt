package com.mediaeditor.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home          : Screen("home")
    object AudioEditor   : Screen("audio_editor?uri={uri}") {
        fun createRoute(uri: String) = "audio_editor?uri=${Uri.encode(uri)}"
    }
    object VideoEditor   : Screen("video_editor?uri={uri}") {
        fun createRoute(uri: String) = "video_editor?uri=${Uri.encode(uri)}"
    }
    object Converter     : Screen("converter")
    object Batch         : Screen("batch")
    object Preferences   : Screen("preferences")
}
