package com.example.assistentemeditechmobile.data

import android.content.Context
import com.example.assistentemeditechmobile.domain.VipContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class VipRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private var cachedVips: List<VipContact>? = null
    private val vipsFile: File
        get() = File(context.filesDir, "vip_contacts.json")

    private val defaultVips = listOf(
        VipContact(
            id = "vip-meystre",
            name = "Prof. Stéphane Meystre",
            emailOrDomain = "stephane.meystre@supsi.ch",
            roleTitle = "Direttore Istituto MeDiTech SUPSI",
            isAutoHighPriority = true
        ),
        VipContact(
            id = "vip-supsi-meditech",
            name = "MeDiTech SUPSI Colleghi",
            emailOrDomain = "supsi.ch",
            roleTitle = "Dipartimento Tecnologie Innovative",
            isAutoHighPriority = true
        ),
        VipContact(
            id = "vip-eoc",
            name = "EOC Ticino (Ospedali Cantonali)",
            emailOrDomain = "eoc.ch",
            roleTitle = "Partner Clinico Cantonale",
            isAutoHighPriority = true
        ),
        VipContact(
            id = "vip-cardiocentro",
            name = "Cardiocentro Ticino",
            emailOrDomain = "cardiocentro.org",
            roleTitle = "Istituto Clinico & Ricerca",
            isAutoHighPriority = true
        )
    )

    suspend fun getVips(): List<VipContact> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!vipsFile.exists()) {
                vipsFile.writeText(json.encodeToString(defaultVips))
                return@withLock defaultVips
            }
            runCatching {
                json.decodeFromString<List<VipContact>>(vipsFile.readText())
            }.getOrElse {
                vipsFile.writeText(json.encodeToString(defaultVips))
                defaultVips
            }
        }
    }

    suspend fun addVip(name: String, emailOrDomain: String, roleTitle: String): VipContact = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getVipsInternal().toMutableList()
            val newVip = VipContact(
                id = UUID.randomUUID().toString().take(8),
                name = name.trim(),
                emailOrDomain = emailOrDomain.trim().lowercase(),
                roleTitle = roleTitle.trim().ifBlank { "Contatto VIP" }
            )
            current.add(0, newVip)
            cachedVips = current
            vipsFile.writeText(json.encodeToString(current))
            newVip
        }
    }

    suspend fun removeVip(vipId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getVipsInternal().toMutableList()
            val removed = current.removeAll { it.id == vipId }
            if (removed) {
                cachedVips = current
                vipsFile.writeText(json.encodeToString(current))
            }
            removed
        }
    }

    suspend fun isVip(sender: String, subject: String = ""): Boolean = withContext(Dispatchers.IO) {
        val vips = getVips()
        com.example.assistentemeditechmobile.domain.VipMatcher.isVip(sender, subject, vips)
    }

    private fun getVipsInternal(): List<VipContact> {
        cachedVips?.let { return it }
        if (!vipsFile.exists()) {
            cachedVips = defaultVips
            return defaultVips
        }
        val loaded = runCatching {
            json.decodeFromString<List<VipContact>>(vipsFile.readText())
        }.getOrDefault(defaultVips)
        cachedVips = loaded
        return loaded
    }
}
