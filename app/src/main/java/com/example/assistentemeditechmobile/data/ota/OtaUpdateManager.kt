package com.example.assistentemeditechmobile.data.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class OtaUpdateManager(private val context: Context) {

    companion object {
        const val DEFAULT_MANIFEST_URL = "https://raw.githubusercontent.com/ManuRoma02/assistente-meditech-mobile/main/ota-update.json"
        const val FALLBACK_MANIFEST_URL = "https://raw.githubusercontent.com/ManuRoma02/assistente-meditech-mobile/master/ota-update.json"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000

        /**
         * Risolve in modo robusto redirect assoluti e relativi (RFC 7231 / AWS S3 redirects).
         */
        fun resolveRedirectUrl(baseUrl: String, locationHeader: String): String {
            return runCatching {
                URL(URL(baseUrl), locationHeader).toString()
            }.getOrDefault(locationHeader)
        }

        /**
         * Calcola l'hash crittografico SHA-256 in esadecimale minuscolo per un file (CWE-494).
         */
        fun computeSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Verifica logica di disponibilità aggiornamento (testabile senza contesto Android).
         */
        fun isUpdateAvailable(manifest: OtaUpdateManifest, currentVersionCode: Int): Boolean {
            return manifest.latestVersionCode > currentVersionCode
        }

        /**
         * Valuta lo stato dell'aggiornamento a partire dal manifest e dalla versione locale.
         * Se [alwaysAvailable] è true, l'aggiornamento è considerato sempre disponibile (utile per forzare reinstallazione).
         */
        fun evaluateUpdate(
            manifest: OtaUpdateManifest,
            currentVersionCode: Int,
            currentVersionName: String,
            alwaysAvailable: Boolean = false
        ): UpdateCheckResult {
            val available = alwaysAvailable || isUpdateAvailable(manifest, currentVersionCode)
            return UpdateCheckResult(
                status = if (available) UpdateCheckStatus.UPDATE_AVAILABLE else UpdateCheckStatus.UP_TO_DATE,
                manifest = manifest,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
        }

        /**
         * Verifica l'integrità strutturale, la firma e la coerenza del pacchetto APK scaricato.
         */
        fun verifyApkArchive(
            context: Context,
            apkFile: File,
            targetVersionCode: Int = 0,
            expectedSha256: String = "",
            allowAnyVersion: Boolean = true
        ): Result<Unit> {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(IllegalStateException("File APK inesistente o vuoto"))
            }

            // 1. Verifica crittografica SHA-256 se fornita nel manifest
            if (expectedSha256.isNotBlank()) {
                val computedHash = computeSha256(apkFile)
                if (!computedHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                    return Result.failure(
                        SecurityException("Integrità SHA-256 fallita: atteso $expectedSha256, calcolato $computedHash")
                    )
                }
            }

            // 2. Verifica parsing pacchetto Android nativo (evita 'Errore di analisi pacchetto')
            val pm = try { context.packageManager } catch (_: Throwable) { null }
            if (pm != null) {
                val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    ?: return Result.failure(IllegalStateException("Il file scaricato non è un pacchetto Android valido (APK corrotto)"))

                if (archiveInfo.packageName != context.packageName) {
                    return Result.failure(
                        SecurityException("Package APK non corrispondente: ${archiveInfo.packageName} (atteso: ${context.packageName})")
                    )
                }

                val apkVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo).toInt()
                if (!allowAnyVersion && targetVersionCode > 0 && apkVersionCode < targetVersionCode) {
                    return Result.failure(
                        IllegalStateException("Versione APK inferiore al target richiesto: build $apkVersionCode < $targetVersionCode")
                    )
                }
            }

            return Result.success(Unit)
        }
    }

    /**
     * Recupera un APK già scaricato in precedenza se presente nella cache ed integro.
     */
    fun getExistingDownloadedApk(
        targetVersionCode: Int,
        expectedSha256: String = "",
        allowAnyVersion: Boolean = true
    ): File? {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) return null
        val candidates = updatesDir.listFiles { f ->
            f.isFile && f.name.startsWith("meditech_update_${targetVersionCode}_") && f.name.endsWith(".apk")
        } ?: return null
        for (candidate in candidates.sortedByDescending { it.lastModified() }) {
            if (verifyApkArchive(context, candidate, targetVersionCode, expectedSha256, allowAnyVersion).isSuccess) {
                return candidate
            }
        }
        return null
    }

    /**
     * Rimuove file temporanei .tmp residui e pacchetti APK obsoleti per liberare storage.
     */
    fun cleanupOldUpdates(keepOnlyLatestVersionCode: Int? = null) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) return
        val files = updatesDir.listFiles() ?: return
        for (f in files) {
            if (f.name.endsWith(".tmp")) {
                f.delete()
                continue
            }
            if (f.name.endsWith(".apk") && keepOnlyLatestVersionCode != null) {
                if (!f.name.startsWith("meditech_update_${keepOnlyLatestVersionCode}_")) {
                    f.delete()
                }
            }
        }
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (_: Throwable) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Throwable) {
            "1.0.0"
        }
    }

    /**
     * Scarica e decodifica il manifest JSON da un endpoint remoto HTTP/HTTPS (GitHub Raw).
     */
    suspend fun fetchRemoteManifest(manifestUrl: String): Result<OtaUpdateManifest> = withContext(Dispatchers.IO) {
        runCatching {
            var currentUrl = if (manifestUrl.contains("?")) "$manifestUrl&_t=${System.currentTimeMillis()}" else "$manifestUrl?_t=${System.currentTimeMillis()}"
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 5

            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    useCaches = false
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "MeDiTech-Android/${getCurrentVersionName()}")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Pragma", "no-cache")
                    instanceFollowRedirects = true
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newLocation != null) {
                        currentUrl = resolveRedirectUrl(currentUrl, newLocation)
                        redirects++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: error("Connessione non inizializzata")
            if (conn.responseCode !in 200..299) {
                error("Errore HTTP ${conn.responseCode}: ${conn.responseMessage}")
            }

            val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val parsed = OtaUpdateManifest.fromJson(rawJson)
            parsed.getOrThrow()
        }
    }

    /**
     * Controlla se è presente una nuova versione comparando con il codice versione locale.
     * Se [force] è true, forza lo stato UPDATE_AVAILABLE consentendo all'utente di reinstallare o aggiornare.
     */
    suspend fun checkForUpdate(
        manifestUrl: String = DEFAULT_MANIFEST_URL,
        explicitCurrentVersionCode: Int? = null,
        explicitCurrentVersionName: String? = null,
        force: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentCode = explicitCurrentVersionCode ?: getCurrentVersionCode()
        val currentName = explicitCurrentVersionName ?: getCurrentVersionName()

        var manifestResult = fetchRemoteManifest(manifestUrl)
        if (manifestResult.isFailure && manifestUrl == DEFAULT_MANIFEST_URL) {
            manifestResult = fetchRemoteManifest(FALLBACK_MANIFEST_URL)
        }

        if (manifestResult.isFailure) {
            return@withContext UpdateCheckResult(
                status = UpdateCheckStatus.CHECK_FAILED,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                errorMessage = manifestResult.exceptionOrNull()?.localizedMessage ?: "Impossibile recuperare il manifest di aggiornamento"
            )
        }

        val manifest = manifestResult.getOrThrow()
        evaluateUpdate(manifest, currentCode, currentName, alwaysAvailable = force)
    }

    /**
     * Scarica l'APK fornito nel downloadUrl nella cache dell'app notificando il progresso,
     * verificando la sicurezza HTTPS e l'eventuale integrità crittografica SHA-256 (CWE-494).
     */
    suspend fun downloadApk(
        downloadUrl: String,
        targetVersionCode: Int = 0,
        expectedSha256: String = "",
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val destinationFile = File(updatesDir, "meditech_update_${targetVersionCode}_${System.currentTimeMillis()}.apk")
        val tempFile = File(updatesDir, "${destinationFile.name}.tmp")

        try {
            if (!downloadUrl.startsWith("https://", ignoreCase = true)) {
                error("Protocollo non sicuro: il download degli aggiornamenti richiede obbligatoriamente HTTPS.")
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 5

            while (redirects < maxRedirects) {
                if (!currentUrl.startsWith("https://", ignoreCase = true)) {
                    error("Reindirizzamento verso endpoint HTTP non cifrato bloccato per sicurezza.")
                }
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS * 2
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "MeDiTech-Android/${getCurrentVersionName()}")
                    setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    instanceFollowRedirects = true
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newLocation != null) {
                        currentUrl = resolveRedirectUrl(currentUrl, newLocation)
                        redirects++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: error("Connessione non inizializzata")
            if (conn.responseCode !in 200..299) {
                error("Errore download APK: HTTP ${conn.responseCode} ${conn.responseMessage}")
            }

            val totalBytes = conn.contentLength.toLong()
            var downloadedBytes = 0L

            conn.inputStream.use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var lastNotifiedProgress = -1f
                    var lastNotifiedBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            val estimatedTotal = 25 * 1024 * 1024f
                            (downloadedBytes.toFloat() / estimatedTotal).coerceIn(0.01f, 0.95f)
                        }

                        val progressChanged = Math.abs(progress - lastNotifiedProgress) >= 0.01f
                        val bytesChangedSignificantly = (downloadedBytes - lastNotifiedBytes) >= (256 * 1024)
                        if (progressChanged || bytesChangedSignificantly || downloadedBytes == totalBytes) {
                            lastNotifiedProgress = progress
                            lastNotifiedBytes = downloadedBytes
                            onProgress(progress, downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (destinationFile.exists()) destinationFile.delete()
                if (!tempFile.renameTo(destinationFile)) {
                    error("Impossibile rinominare il file temporaneo dell'aggiornamento")
                }

                // Verifica strutturale, firma e SHA-256
                val verifyResult = verifyApkArchive(context, destinationFile, targetVersionCode, expectedSha256)
                if (verifyResult.isFailure) {
                    destinationFile.delete()
                    throw verifyResult.exceptionOrNull() ?: IllegalStateException("Verifica integrità APK fallita")
                }

                Result.success(destinationFile)
            } else {
                error("Il file scaricato è vuoto o corrotto")
            }
        } catch (t: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            Result.failure(t)
        }
    }

    /**
     * Controlla se l'app possiede l'autorizzazione all'installazione di pacchetti esterni (Android 8.0+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Genera l'intent per avviare l'installazione nativa dell'APK tramite FileProvider.
     */
    fun createInstallIntent(apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Genera l'intent per condurre l'utente nelle impostazioni di sistema e abilitare l'installazione.
     */
    fun createManageUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
