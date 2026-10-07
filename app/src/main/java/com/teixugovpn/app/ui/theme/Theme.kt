package com.teixugovpn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TeixugoGold = Color(0xFFF5B725)
val TeixugoGoldDark = Color(0xFFD89D00)
val TeixugoBlack = Color(0xFF101010)
val TeixugoBackground = Color(0xFF17171A)
val TeixugoSurface = Color(0xFF1F1F22)
val TeixugoTextMuted = Color(0xFFA6A39C)
val TeixugoGreen = Color(0xFF40D866)
val TeixugoRed = Color(0xFFFF4B42)

private val TeixugoColorScheme = darkColorScheme(
    primary = TeixugoGold,
    onPrimary = TeixugoBlack,
    secondary = TeixugoGoldDark,
    background = TeixugoBackground,
    surface = TeixugoSurface,
    error = TeixugoRed,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun TeixugoVpnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TeixugoColorScheme,
        content = content,
    )
}
