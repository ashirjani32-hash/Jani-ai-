package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.security.configureAppCheck
import com.example.ui.AppNavigation
import com.example.ui.theme.JaniAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            JaniAiTheme {
                AppNavigation()
            }
        }
    }
}
