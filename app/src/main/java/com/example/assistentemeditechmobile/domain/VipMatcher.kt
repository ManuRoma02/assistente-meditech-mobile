package com.example.assistentemeditechmobile.domain

object VipMatcher {
    fun isVip(sender: String, subject: String = "", vips: List<VipContact>): Boolean {
        val check = "$sender $subject".lowercase()
        return vips.any { vip ->
            val pattern = vip.emailOrDomain.lowercase().trim()
            (pattern.isNotBlank() && check.contains(pattern)) ||
            (vip.name.isNotBlank() && check.contains(vip.name.lowercase().trim()))
        }
    }
}
