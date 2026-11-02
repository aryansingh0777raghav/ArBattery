package com.arverse.arbattery.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.arverse.arbattery.core.designsystem.theme.ArBatteryTheme

class CompanionMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArBatteryTheme {
                val viewModel = remember {
                    CompanionViewModel(appContext = applicationContext)
                }
                CompanionScreen(viewModel = viewModel)
            }
        }
    }
}
