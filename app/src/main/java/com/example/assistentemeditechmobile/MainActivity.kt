package com.example.assistentemeditechmobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.assistentemeditechmobile.theme.AssistenteMeditechMobileTheme
import com.example.assistentemeditechmobile.ui.AssistenteMeditechApp
import com.example.assistentemeditechmobile.ui.MeditechViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MeditechViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        viewModel.refreshEasterEggForAppOpen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        setContent {
            AssistenteMeditechMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AssistenteMeditechApp(viewModel = viewModel)
                }
            }
        }
    }
}
