package co.geoluker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = LukerBrown,
    onPrimary = SurfaceLight,
    primaryContainer = LukerSand,
    onPrimaryContainer = LukerBrown,
    secondary = LukerBrownSoft,
    onSecondary = SurfaceLight,
    tertiary = PointGold,
    background = CanvasLight,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = LukerSand,
    onSurfaceVariant = MutedInk,
    outline = BorderLight,
    error = AlertTerracotta,
)

private val DarkColors = darkColorScheme(
    primary = ColorTokens.BrownNight,
    onPrimary = LukerBrown,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkText,
    secondary = ColorTokens.SandNight,
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
    val BrownNight = androidx.compose.ui.graphics.Color(0xFFE2A879)
    val SandNight = androidx.compose.ui.graphics.Color(0xFFD7B99F)
    val GoldNight = androidx.compose.ui.graphics.Color(0xFFF0AD52)
    val BorderNight = androidx.compose.ui.graphics.Color(0xFF594235)
    val AlertNight = androidx.compose.ui.graphics.Color(0xFFEB8F75)
}

private val GeoLukerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GeoLukerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GeoLukerTypography,
        shapes = GeoLukerShapes,
        content = content,
    )
}
