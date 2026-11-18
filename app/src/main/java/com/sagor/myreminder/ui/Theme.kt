package com.sagor.myreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF7FAAFF))
    } else {
        lightColorScheme(
            primary = Color(0xFF1E6BFF),
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5FD)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
