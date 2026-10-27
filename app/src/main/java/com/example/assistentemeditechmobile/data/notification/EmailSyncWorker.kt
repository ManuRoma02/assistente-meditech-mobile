package com.example.assistentemeditechmobile.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.assistentemeditechmobile.data.EmailRepository
import com.example.assistentemeditechmobile.data.VipRepository

class EmailSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val vipRepository = VipRepository(applicationContext)
            val emailRepository = EmailRepository(applicationContext, vipRepository)
            val emails = emailRepository.getEmails()

            // Controlla se c'è un'email non letta recente ricevuta nell'ultima ora
            val recentUnread = emails.firstOrNull { !it.isRead && System.currentTimeMillis() - it.timestampMillis < 3600_000L }
            if (recentUnread != null) {
                EmailNotificationManager.showEmailNotification(applicationContext, recentUnread)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
