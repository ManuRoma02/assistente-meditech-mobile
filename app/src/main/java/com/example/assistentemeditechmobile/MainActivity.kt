package com.example.assistentemeditechmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistentemeditechmobile.data.notification.EmailNotificationManager
import com.example.assistentemeditechmobile.data.security.MeditechBiometricManager
import com.example.assistentemeditechmobile.theme.AssistenteMeditechMobileTheme
import com.example.assistentemeditechmobile.ui.AssistenteMeditechApp
import com.example.assistentemeditechmobile.ui.MeditechViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MeditechViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            EmailNotificationManager.createNotificationChannel(this)
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permesso gestito */ }

    override fun onStart() {
        super.onStart()
        viewModel.refreshEasterEggForAppOpen()
    }

    override fun onStop() {
        super.onStop()
        viewModel.lockApp()
    }

    override fun onResume() {
        super.onResume()
        checkBiometricOnResume()
    }

    private fun checkBiometricOnResume() {
        val state = viewModel.uiState.value
        if (state.biometricLockEnabled && !state.isAppUnlocked && MeditechBiometricManager.isBiometricAvailable(this)) {
            MeditechBiometricManager.promptBiometric(
                activity = this,
                onSuccess = { viewModel.unlockApp() },
                onError = { /* Utente può ritentare */ }
            )
        }
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

        // Inizializza canali notifiche ed esegue request runtime su Android 13+
        requestNotificationPermissionIfNeeded()
        requestAudioPermissionIfNeeded()

        // Schedula controllo email periodico con vincolo batteria efficiente
        scheduleBackgroundEmailSync()

        // Gestione eventuale apertura da notifica
        handleNotificationIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = when (uiState.themeMode) {
                "Chiaro" -> false
                "Scuro" -> true
                else -> isSystemInDarkTheme()
            }

            AssistenteMeditechMobileTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AssistenteMeditechApp(
                        viewModel = viewModel,
                        onRequestBiometricPrompt = {
                            if (MeditechBiometricManager.isBiometricAvailable(this@MainActivity)) {
                                MeditechBiometricManager.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = { viewModel.unlockApp() },
                                    onError = { /* Retry */ }
                                )
                            }
                        }
                    )
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

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
