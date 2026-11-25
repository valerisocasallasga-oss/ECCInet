package co.edu.eccinet

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EcciBlue = Color(0xFF0844AC)
val EcciDark = Color(0xFF003B9D)
val EcciYellow = Color(0xFFFFBC13)
val EcciBackground = Color(0xFFF4F6FC)
val EcciText = Color(0xFF152136)
val EcciMuted = Color(0xFF79849A)

@Composable
fun ECCInetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = EcciBlue,
            secondary = EcciYellow,
            background = EcciBackground,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = EcciText
        ),
        content = content
    )
}
