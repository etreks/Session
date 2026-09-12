package com.satyam.session

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.satyam.session.navigation.SessionNavigation
import com.satyam.session.ui.theme.SessionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SessionTheme {
                SessionNavigation()
            }
        }
    }
}
