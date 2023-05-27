package com.example.lightsapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.lightsapp.ui.mainScreen.MainScreen
import com.example.lightsapp.ui.theme.LightsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightsAppTheme {
                MainScreen()
            }
        }
    }
}