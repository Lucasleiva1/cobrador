package com.cajasimple.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cajasimple.app.domain.model.ThemeMode
import com.cajasimple.app.domain.model.VisualTheme

private val Cream = Color(0xFFFAF7F0)
private val Ink = Color(0xFF171717)
private val Blue = Color(0xFF1457D9)
private val Navy = Color(0xFF0B2545)
private val Red = Color(0xFFD72C2C)
private val DarkCreamBackground = Color(0xFF12110F)
private val DarkCreamSurface = Color(0xFF1D1A16)
private val DarkCreamText = Color(0xFFF3EEE5)
private val DarkCreamMuted = Color(0xFFCFC7BB)

@Composable
fun CajaTheme(theme: VisualTheme, mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK
    val colors = if (dark) darkColors(theme) else lightColors(theme)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp),
        ),
        content = content,
    )
}

private fun lightColors(theme: VisualTheme): ColorScheme = when (theme) {
    VisualTheme.MONO -> lightColorScheme(
        primary = Ink, onPrimary = Color.White, primaryContainer = Color(0xFFE8E8E8), onPrimaryContainer = Ink,
        background = Color.White, surface = Color.White, surfaceVariant = Color(0xFFF0F0F0),
        onBackground = Ink, onSurface = Ink, onSurfaceVariant = Color(0xFF4A4A4A), outline = Color(0xFF747474),
        error = Red, onError = Color.White,
    )
    VisualTheme.BLUE -> lightColorScheme(
        primary = Blue, onPrimary = Color.White, primaryContainer = Color(0xFFDCE6FF), onPrimaryContainer = Color(0xFF001A43),
        background = Color(0xFFF7F9FE), surface = Color.White, surfaceVariant = Color(0xFFE6EAF2),
        onBackground = Ink, onSurface = Ink, onSurfaceVariant = Color(0xFF44474F), outline = Color(0xFF74777F),
        error = Red, onError = Color.White,
    )
    VisualTheme.RED_BLACK -> lightColorScheme(
        primary = Red, onPrimary = Color.White, primaryContainer = Color(0xFFFFDAD5), onPrimaryContainer = Color(0xFF410001),
        background = Color(0xFFFFF8F7), surface = Color.White, surfaceVariant = Color(0xFFF5DDDA),
        onBackground = Ink, onSurface = Ink, onSurfaceVariant = Color(0xFF534341), outline = Color(0xFF857370),
        error = Red, onError = Color.White,
    )
    VisualTheme.CREAM -> lightColorScheme(
        primary = Ink, onPrimary = Color.White, primaryContainer = Color(0xFFE9E2D6), onPrimaryContainer = Ink,
        background = Cream, surface = Color(0xFFFFFCF7), surfaceVariant = Color(0xFFEAE3D8),
        onBackground = Ink, onSurface = Ink, onSurfaceVariant = Color(0xFF4E4942), outline = Color(0xFF7F786E),
        error = Red, onError = Color.White,
    )
    VisualTheme.NAVY_CREAM -> lightColorScheme(
        primary = Navy, onPrimary = Cream, primaryContainer = Color(0xFFD5E3FF), onPrimaryContainer = Color(0xFF001B3A),
        background = Cream, surface = Color(0xFFFFFCF7), surfaceVariant = Color(0xFFE1E7EF),
        onBackground = Navy, onSurface = Navy, onSurfaceVariant = Color(0xFF414750), outline = Color(0xFF727781),
        error = Red, onError = Color.White,
    )
}

private fun darkColors(theme: VisualTheme): ColorScheme = when (theme) {
    VisualTheme.MONO -> darkColorScheme(
        primary = Color(0xFFE4E4E4), onPrimary = Color(0xFF202020), primaryContainer = Color(0xFF373737), onPrimaryContainer = Color.White,
        background = Color(0xFF101010), surface = Color(0xFF191919), surfaceVariant = Color(0xFF2A2A2A),
        onBackground = Color(0xFFF1F1F1), onSurface = Color(0xFFF1F1F1), onSurfaceVariant = Color(0xFFC8C8C8), outline = Color(0xFF929292),
        error = Color(0xFFFF8A80), onError = Color(0xFF4B0003),
    )
    VisualTheme.BLUE -> darkColorScheme(
        primary = Color(0xFFAEC6FF), onPrimary = Color(0xFF002E6D), primaryContainer = Color(0xFF0A438F), onPrimaryContainer = Color(0xFFDCE6FF),
        background = Color(0xFF0B0F17), surface = Color(0xFF121824), surfaceVariant = Color(0xFF28303D),
        onBackground = Color(0xFFE3E8F2), onSurface = Color(0xFFE3E8F2), onSurfaceVariant = Color(0xFFC3C8D2), outline = Color(0xFF8D919B),
        error = Color(0xFFFF8A80), onError = Color(0xFF4B0003),
    )
    VisualTheme.RED_BLACK -> darkColorScheme(
        primary = Color(0xFFC62828), onPrimary = Color.White, primaryContainer = Color(0xFF761D1D), onPrimaryContainer = Color.White,
        secondary = Color(0xFFB83A3A), onSecondary = Color.White, secondaryContainer = Color(0xFF721F1F), onSecondaryContainer = Color.White,
        tertiary = Color(0xFFE0E0E0), onTertiary = Color(0xFF242424), tertiaryContainer = Color(0xFF3A3A3A), onTertiaryContainer = Color(0xFFF5F5F5),
        background = Color(0xFF151515), surface = Color(0xFF1E1E1E), surfaceVariant = Color(0xFF2B2B2B),
        onBackground = Color(0xFFF5F5F5), onSurface = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFFD0D0D0), outline = Color(0xFF8A8A8A),
        error = Color(0xFFD32F2F), onError = Color.White,
    )
    VisualTheme.CREAM -> darkColorScheme(
        primary = Color(0xFFEBD29C), onPrimary = Color(0xFF3D2E00), primaryContainer = Color(0xFF584500), onPrimaryContainer = Color(0xFFFFE7AD),
        background = DarkCreamBackground, surface = DarkCreamSurface, surfaceVariant = Color(0xFF2C2823),
        onBackground = DarkCreamText, onSurface = DarkCreamText, onSurfaceVariant = DarkCreamMuted, outline = Color(0xFF978F83),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    )
    VisualTheme.NAVY_CREAM -> darkColorScheme(
        primary = Color(0xFFF0D99F), onPrimary = Color(0xFF3B2F00), primaryContainer = Color(0xFF254B70), onPrimaryContainer = Color(0xFFD5E3FF),
        background = Color(0xFF071522), surface = Color(0xFF0D2133), surfaceVariant = Color(0xFF24384A),
        onBackground = Color(0xFFF4EBD8), onSurface = Color(0xFFF4EBD8), onSurfaceVariant = Color(0xFFC2C8D0), outline = Color(0xFF8C929B),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    )
}
