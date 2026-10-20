package com.example.assistentemeditechmobile

import com.example.assistentemeditechmobile.domain.VipContact
import com.example.assistentemeditechmobile.domain.VipMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VipMatchingTest {

    private val vips = listOf(
        VipContact(
            id = "vip-meystre",
            name = "Prof. Stéphane Meystre",
            emailOrDomain = "stephane.meystre@supsi.ch",
            roleTitle = "Direttore MeDiTech"
        ),
        VipContact(
            id = "vip-supsi",
            name = "SUPSI MeDiTech Team",
            emailOrDomain = "supsi.ch",
            roleTitle = "Ricercatori MeDiTech"
        ),
        VipContact(
            id = "vip-eoc",
            name = "EOC Ticino",
            emailOrDomain = "eoc.ch",
            roleTitle = "Partner Ospedaliero"
        )
    )

    @Test
    fun testDirectEmailMatchReturnsTrue() {
        val sender = "Prof. Stéphane Meystre <stephane.meystre@supsi.ch>"
        assertTrue(VipMatcher.isVip(sender = sender, subject = "Riunione", vips = vips))
    }

    @Test
    fun testDomainMatchReturnsTrue() {
        val sender = "ricercatore.junior@supsi.ch"
        assertTrue(VipMatcher.isVip(sender = sender, subject = "Protocollo sperimentale", vips = vips))
    }

    @Test
    fun testPartnerHospitalDomainReturnsTrue() {
        val sender = "Direzione Medica <segreteria@eoc.ch>"
        assertTrue(VipMatcher.isVip(sender = sender, subject = "Comitato Etico", vips = vips))
    }

    @Test
    fun testNameMatchInSubjectOrBodyReturnsTrue() {
        val sender = "esterno@unil.ch"
        val subject = "Incontro congiunto con Prof. Stéphane Meystre"
        assertTrue(VipMatcher.isVip(sender = sender, subject = subject, vips = vips))
    }

    @Test
    fun testExternalNewsletterReturnsFalse() {
        val sender = "IEEE Digital Health <newsletter@ieee-org.org>"
        val subject = "Conference Call for Papers 2026"
        assertFalse(VipMatcher.isVip(sender = sender, subject = subject, vips = vips))
    }
}
