package co.georefer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = Sage,
    onPrimaryContainer = ForestDeep,
    secondary = Moss,
    onSecondary = SurfaceLight,
    tertiary = FieldGold,
    background = CanvasLight,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = Sage,
    onSurfaceVariant = MutedInk,
    outline = BorderLight,
    error = AlertTerracotta,
)

private val DarkColors = darkColorScheme(
    primary = ColorTokens.ForestNight,
    onPrimary = ForestDeep,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkText,
    secondary = ColorTokens.MossNight,
    tertiary = ColorTokens.GoldNight,
    background = DarkCanvas,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = ColorTokens.BorderNight,
    error = ColorTokens.AlertNight,
)

private object ColorTokens {
    val ForestNight = androidx.compose.ui.graphics.Color(0xFF55BD8D)
    val MossNight = androidx.compose.ui.graphics.Color(0xFF85AB81)
    val GoldNight = androidx.compose.ui.graphics.Color(0xFFF0AD52)
    val BorderNight = androidx.compose.ui.graphics.Color(0xFF30463A)
    val AlertNight = androidx.compose.ui.graphics.Color(0xFFEB8F75)
}

private val GeoreferShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GeoreferTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GeoreferTypography,
        shapes = GeoreferShapes,
        content = content,
    )
}
