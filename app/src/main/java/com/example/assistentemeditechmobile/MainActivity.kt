package com.example.assistentemeditechmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.assistentemeditechmobile.data.notification.EmailNotificationManager
import com.example.assistentemeditechmobile.theme.AssistenteMeditechMobileTheme
import com.example.assistentemeditechmobile.ui.AssistenteMeditechApp
import com.example.assistentemeditechmobile.ui.MeditechViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MeditechViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            EmailNotificationManager.createNotificationChannel(this)
        }
    }

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

        // Inizializza canale notifiche ed esegue request runtime su Android 13+
        requestNotificationPermissionIfNeeded()

        // Schedula controllo email periodico con vincolo batteria efficiente
        scheduleBackgroundEmailSync()

        // Gestione eventuale apertura da notifica
        handleNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val emailId = intent?.getStringExtra(EmailNotificationManager.EXTRA_EMAIL_ID)
        if (!emailId.isNullOrBlank()) {
            viewModel.openEmailById(emailId)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        EmailNotificationManager.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleBackgroundEmailSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.assistentemeditechmobile.data.notification.EmailSyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "MeditechEmailSyncWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
