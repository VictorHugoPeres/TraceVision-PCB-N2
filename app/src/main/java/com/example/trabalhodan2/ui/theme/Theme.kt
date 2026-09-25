package com.example.trabalhodan2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val IndustrialLightColorScheme = lightColorScheme(
    primary = TechPrimary,
    onPrimary = TechSurface,
    primaryContainer = TechPrimaryContainer,
    onPrimaryContainer = TechTextPrimary,
    background = TechBackground,
    onBackground = TechTextPrimary,
    surface = TechSurface,
    onSurface = TechTextPrimary,
    surfaceVariant = TechSurfaceVariant,
    onSurfaceVariant = TechTextSecondary,
    outline = TechBorder,
    error = TechAlertSevere
)

@Composable
fun TrabalhoDaN2Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IndustrialLightColorScheme,
        typography = Typography,
        content = content
    )
}
