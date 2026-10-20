package com.example.assistentemeditechmobile.data

import android.content.Context
import com.example.assistentemeditechmobile.domain.ClinicalProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ClinicalProjectRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private val projectsFile: File
        get() = File(context.filesDir, "meditech_projects.json")

    private val defaultProjects = listOf(
        ClinicalProject(
            id = "proj-nlp-ehr",
            codice = "MDT-2026-NLP",
            titolo = "NLP Clinico & Estrazione Dati Cartelle Elettroniche",
            istituto = "SUPSI Istituto MeDiTech (Lugano)",
            responsabileScientifico = "Prof. Stéphane Meystre",
            stato = "In Corso",
            descrizione = "Sviluppo di modelli linguistici e architetture di Information Extraction per referti clinici e cartelle ospedaliere in conformità GDPR/LPD.",
            documentCount = 14
        ),
        ClinicalProject(
            id = "proj-biosensors",
            codice = "MDT-2026-BIO",
            titolo = "Dispositivi Medici Indossabili & Monitoraggio Remoto",
            istituto = "SUPSI MeDiTech / EOC Ticino",
            responsabileScientifico = "Dr.ssa Elena Fontana & MeDiTech Team",
            stato = "Fase Clinica II",
            descrizione = "Validazione clinica di biosensori a basso consumo per monitoraggio continuo di parametri vitali in pazienti cronici domiciliari.",
            documentCount = 8
        ),
        ClinicalProject(
            id = "proj-ai-triage",
            codice = "MDT-2025-TRI",
            titolo = "Supporto Decisionale AI per Triage Ospedaliero",
            istituto = "SUPSI MeDiTech / Ospedale Regionale Lugano",
            responsabileScientifico = "Prof. Stéphane Meystre",
            stato = "Validazione Finale",
            descrizione = "Algoritmi predittivi per prioritizzazione del percorso di pronto soccorso e riduzione dei tempi di attesa clinica.",
            documentCount = 19
        )
    )

    suspend fun getProjects(): List<ClinicalProject> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!projectsFile.exists()) {
                projectsFile.writeText(json.encodeToString(defaultProjects))
                return@withLock defaultProjects
            }
            runCatching {
                json.decodeFromString<List<ClinicalProject>>(projectsFile.readText())
            }.getOrElse {
                projectsFile.writeText(json.encodeToString(defaultProjects))
                defaultProjects
            }
        }
    }

    suspend fun addProject(titolo: String, codice: String, descrizione: String): ClinicalProject = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getProjectsInternal().toMutableList()
            val newP = ClinicalProject(
                id = "proj-" + UUID.randomUUID().toString().take(6),
                codice = codice.trim().ifBlank { "MDT-2026-${(100..999).random()}" },
                titolo = titolo.trim(),
                descrizione = descrizione.trim(),
                documentCount = 1
            )
            current.add(0, newP)
            projectsFile.writeText(json.encodeToString(current))
            newP
        }
    }

    private fun getProjectsInternal(): List<ClinicalProject> {
        if (!projectsFile.exists()) return defaultProjects
        return runCatching {
            json.decodeFromString<List<ClinicalProject>>(projectsFile.readText())
        }.getOrDefault(defaultProjects)
    }
}
