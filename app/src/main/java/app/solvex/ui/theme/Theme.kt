package app.solvex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FireOrange,
    secondary = WaterBlue,
    tertiary = WaterCyan,
    background = DarkBg,
    surface = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = FireGlow,
    secondary = WaterDeep,
    tertiary = WaterGlow,
    background = LightBg,
    surface = LightSurface
)

@Composable
fun SolvexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
