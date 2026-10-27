package com.example.assistentemeditechmobile.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SecurityHardeningTest {

    @Test
    fun testFilePathsXmlDoesNotExposeRoot() {
        val file = File("src/main/res/xml/file_paths.xml")
        assertTrue("file_paths.xml must exist", file.exists())
        val content = file.readText()

        // Verify root path="." is not exposed
        assertFalse("file_paths.xml must not expose root path='.'", content.contains("path=\".\""))
        assertTrue("file_paths.xml must contain isolated meditech_archive", content.contains("path=\"meditech_archive/\""))
        assertTrue("file_paths.xml must contain isolated updates", content.contains("path=\"updates/\""))
    }

    @Test
    fun testNetworkSecurityConfigRestrictsCleartext() {
        val file = File("src/main/res/xml/network_security_config.xml")
        assertTrue("network_security_config.xml must exist", file.exists())
        val content = file.readText()

        assertTrue("base-config must forbid cleartext", content.contains("cleartextTrafficPermitted=\"false\""))
        assertTrue("domain localhost permitted for local LLM", content.contains("<domain includeSubdomains=\"true\">localhost</domain>"))
        assertTrue("domain 127.0.0.1 permitted for local Ollama", content.contains("<domain includeSubdomains=\"true\">127.0.0.1</domain>"))
        assertTrue("domain local permitted for LAN mDNS", content.contains("<domain includeSubdomains=\"true\">local</domain>"))
    }

    @Test
    fun testBackupRulesExcludesSensitiveClinicalFiles() {
        val file = File("src/main/res/xml/backup_rules.xml")
        assertTrue("backup_rules.xml must exist", file.exists())
        val content = file.readText()

        assertTrue(content.contains("meditech_calendar_events.json"))
        assertTrue(content.contains("meditech_emails.json"))
        assertTrue(content.contains("meditech_chat_history.json"))
        assertTrue(content.contains("email_accounts.json"))
    }

    @Test
    fun testDataExtractionRulesExcludesSensitiveClinicalFiles() {
        val file = File("src/main/res/xml/data_extraction_rules.xml")
        assertTrue("data_extraction_rules.xml must exist", file.exists())
        val content = file.readText()

        assertTrue(content.contains("meditech_calendar_events.json"))
        assertTrue(content.contains("meditech_emails.json"))
        assertTrue(content.contains("meditech_chat_history.json"))
        assertTrue(content.contains("email_accounts.json"))
    }
}
