package com.mediaeditor.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val HybridDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFFFFFFF),   // Stark white — primary actions
    onPrimary        = Color(0xFF000000),   // Black text on white buttons
    primaryContainer = Color(0xFF1C1C1E),   // Subtle container surfaces
    onPrimaryContainer = Color(0xFFF5F5F5),

    secondary        = Color(0xFF8E8E93),   // iOS-like secondary gray
    onSecondary      = Color(0xFFFFFFFF),

    background       = Color(0xFF000000),   // True black (AMOLED-friendly)
    onBackground     = Color(0xFFFFFFFF),

    surface          = Color(0xFF1C1C1E),   // iOS dark surface gray
    onSurface        = Color(0xFFEBEBF5),
    surfaceVariant   = Color(0xFF2C2C2E),   // Slightly elevated surface
    onSurfaceVariant = Color(0xFFAEAEB2),

    outline          = Color(0xFF38383A),   // 1dp border lines (Fluent-style depth)
    outlineVariant   = Color(0xFF2C2C2E),   // Subtle separator
    
    error            = Color(0xFFFF453A),   // iOS red
    onError          = Color(0xFFFFFFFF),
    
    scrim            = Color(0x99000000)    // Semi-transparent modal backdrop
)

val HybridLightColorScheme = lightColorScheme(
    primary          = Color(0xFF000000),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2F2F7),   // iOS grouped background
    onPrimaryContainer = Color(0xFF1C1C1E),

    secondary        = Color(0xFF6E6E73),
    onSecondary      = Color(0xFFFFFFFF),

    background       = Color(0xFFF2F2F7),   // iOS system grouped background
    onBackground     = Color(0xFF000000),

    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF000000),
    surfaceVariant   = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6E6E73),

    outline          = Color(0xFFC6C6C8),
    outlineVariant   = Color(0xFFE5E5EA),
    
    error            = Color(0xFFFF3B30),
    onError          = Color(0xFFFFFFFF)
)
