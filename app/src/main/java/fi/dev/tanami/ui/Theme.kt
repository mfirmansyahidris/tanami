package fi.dev.tanami.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Forest = Color(0xFF174C36)
private val Leaf = Color(0xFF3D805A)
private val Cream = Color(0xFFF5F6EF)
private val Paper = Color(0xFFFFFEFA)
private val Ink = Color(0xFF18231C)
private val Muted = Color(0xFF69756C)
private val PaleGreen = Color(0xFFE7F0E6)

private val TanamiColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = PaleGreen,
    onPrimaryContainer = Forest,
    secondary = Leaf,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF0E8),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD8DED4),
    error = Color(0xFFB23A32)
)

@Composable
fun TanamiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TanamiColors,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(30.dp)
        ),
        content = content
    )
}
