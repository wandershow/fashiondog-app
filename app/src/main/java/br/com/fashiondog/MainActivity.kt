package br.com.fashiondog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.fashiondog.ui.navigation.FashionDogApp
import br.com.fashiondog.ui.theme.FashionDogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FashionDogTheme {
                FashionDogApp()
            }
        }
    }
}
