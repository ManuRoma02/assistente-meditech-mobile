package com.example.assistentemeditechmobile

import com.example.assistentemeditechmobile.data.ota.OtaUpdateManager
import com.example.assistentemeditechmobile.data.ota.OtaUpdateManifest
import com.example.assistentemeditechmobile.data.ota.UpdateCheckStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateTest {

    private val sampleManifest = OtaUpdateManifest(
        latestVersionCode = 2,
        latestVersionName = "1.0.1",
        downloadUrl = "https://github.com/ManuRoma02/assistente-meditech-mobile/releases/download/v1.0.1/app-release.apk",
        releaseNotes = "Nuova release con fix agenda",
        mandatory = false
    )

    @Test
    fun testOlderVersionDetectsUpdateAvailable() {
        val currentVersionCode = 1
        val isAvailable = OtaUpdateManager.isUpdateAvailable(sampleManifest, currentVersionCode)
        assertTrue("When latest version (2) is higher than current (1), update is available", isAvailable)

        val result = OtaUpdateManager.evaluateUpdate(sampleManifest, currentVersionCode, "1.0.0")
        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, result.status)
        assertEquals("1.0.1", result.manifest?.latestVersionName)
    }

    @Test
    fun testSameOrNewerVersionReportsUpToDate() {
        val currentVersionCode = 2
        val isAvailable = OtaUpdateManager.isUpdateAvailable(sampleManifest, currentVersionCode)
        assertFalse("When current version equals latest version, no update available", isAvailable)

        val result = OtaUpdateManager.evaluateUpdate(sampleManifest, currentVersionCode, "1.0.1")
        assertEquals(UpdateCheckStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun testFutureLocalVersionReportsUpToDate() {
        val currentVersionCode = 5 // Local dev build
        val isAvailable = OtaUpdateManager.isUpdateAvailable(sampleManifest, currentVersionCode)
        assertFalse("When current version is ahead of remote, no update available", isAvailable)
    }

    @Test
    fun testManifestSerializationFromJson() {
        val rawJson = """
            {
              "latestVersionCode": 3,
              "latestVersionName": "1.0.2",
              "downloadUrl": "https://example.com/apk.apk",
              "releaseNotes": "Fixes",
              "mandatory": true
            }
        """.trimIndent()

        val parsed = OtaUpdateManifest.fromJson(rawJson).getOrNull()
        assertTrue(parsed != null)
        assertEquals(3, parsed!!.latestVersionCode)
        assertEquals("1.0.2", parsed.latestVersionName)
        assertTrue(parsed.mandatory)
    }

    @Test
    fun testRealOtaUpdateJsonDetectsVersion4AvailableFromCurrentVersion3() {
        val otaJson = """
        {
          "latestVersionCode": 4,
          "latestVersionName": "1.2.1",
          "downloadUrl": "https://github.com/ManuRoma02/assistente-meditech-mobile/releases/download/v1.2.1/Assistente_Meditech_Mobile.apk",
          "releaseNotes": "• Configurazione Email\n• Apple Design\n• Recap Intelligente",
          "mandatory": false,
          "publishedAt": "2026-10-22T10:45:00Z"
        }
        """.trimIndent()

        val parsed = OtaUpdateManifest.fromJson(otaJson).getOrNull()
        assertNotNull(parsed)
        assertEquals(4, parsed!!.latestVersionCode)
        assertEquals("1.2.1", parsed.latestVersionName)

        val currentVersionCode = 3
        val currentVersionName = "1.2.0"
        val eval = OtaUpdateManager.evaluateUpdate(parsed, currentVersionCode, currentVersionName)
        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, eval.status)
        assertEquals("1.2.1", eval.manifest?.latestVersionName)
        assertTrue(eval.manifest?.downloadUrl?.endsWith(".apk") == true)
    }
}
