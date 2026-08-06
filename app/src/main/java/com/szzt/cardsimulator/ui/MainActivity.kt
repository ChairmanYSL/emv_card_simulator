package com.szzt.cardsimulator.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.szzt.cardsimulator.ui.navigation.AppNavigation
import com.szzt.cardsimulator.ui.theme.CardSimulatorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardSimulatorTheme {
                AppNavigation()
            }
        }
    }
}
