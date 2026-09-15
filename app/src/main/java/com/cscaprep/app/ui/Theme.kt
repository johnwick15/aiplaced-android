package com.cscaprep.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF1578B8)
val Navy = Color(0xFF153B5B)
val Pale = Color(0xFFF3F8FC)
val Green = Color(0xFF2E9D62)
val Amber = Color(0xFFE69A23)
val Muted = Color(0xFF667784)
private val Scheme = lightColorScheme(primary = Blue, onPrimary = Color.White, secondary = Navy, background = Pale, onBackground = Color(0xFF17232B), surface = Color.White, onSurface = Color(0xFF17232B), surfaceVariant = Color(0xFFE7F0F7), onSurfaceVariant = Muted, outline = Color(0xFFB8C9D5), error = Color(0xFFB3261E))
@Composable fun CSCAPrepTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = Scheme, content = content) }
