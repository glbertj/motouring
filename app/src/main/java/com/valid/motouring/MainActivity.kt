package com.valid.motouring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.valid.motouring.di.AppContainer
import com.valid.motouring.navigation.MotouringNavHost
import com.valid.motouring.ui.theme.MotouringTheme

class MainActivity : ComponentActivity() {
    private val appContainer = AppContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotouringTheme {
                MotouringNavHost(appContainer = appContainer)
            }
        }
    }
}
