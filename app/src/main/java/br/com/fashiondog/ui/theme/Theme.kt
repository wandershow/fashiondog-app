package br.com.fashiondog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CoresFashionDog = lightColorScheme(
    primary = Amarelo,
    onPrimary = Tinta,
    primaryContainer = AmareloClaro,
    onPrimaryContainer = Tinta,
    secondary = Tinta,
    onSecondary = Branco,
    // a barra de navegação usa secondaryContainer para marcar a aba selecionada
    secondaryContainer = Amarelo,
    onSecondaryContainer = Tinta,
    background = Fundo,
    onBackground = Tinta,
    surface = Branco,
    onSurface = Tinta,
    surfaceVariant = Creme,
    onSurfaceVariant = TextoSuave,
    outline = Borda,
    outlineVariant = Borda,
    error = Erro,
    onError = Branco,
)

@Composable
fun FashionDogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoresFashionDog,
        typography = Typography,
        content = content,
    )
}
