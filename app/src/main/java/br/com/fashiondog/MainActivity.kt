package br.com.fashiondog

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.fashiondog.ui.navigation.RaizDoApp
import br.com.fashiondog.ui.theme.FashionDogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O app é sempre claro: ícones escuros na barra de status e de navegação,
        // mesmo com o celular em modo escuro (senão a hora e a bateria somem no fundo claro).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            FashionDogTheme {
                RaizDoApp()
            }
        }
    }
}
