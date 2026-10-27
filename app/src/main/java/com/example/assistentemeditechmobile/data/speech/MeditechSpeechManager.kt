package com.example.assistentemeditechmobile.data.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Gestore Nativo del Riconoscimento Vocale On-Device per MeDiTech Mobile:
 * - Supporto per l'italiano (Locale.ITALIAN)
 * - Flag EXTRA_PREFER_OFFLINE per garantire l'elaborazione locale
 * - Callback reattive per risultati parziali, finali ed errori
 */
class MeditechSpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit = {},
        onError: (String) -> Unit = {},
        onListeningStateChanged: (Boolean) -> Unit = {}
    ) {
        if (!hasRecordAudioPermission()) {
            onError("Permesso microfono non concesso. Abilitalo nelle impostazioni.")
            return
        }

        if (!isAvailable()) {
            onError("Riconoscimento vocale di sistema non disponibile sul dispositivo.")
            return
        }

        try {
            stopListening()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        onListeningStateChanged(false)
                    }

                    override fun onError(errorCode: Int) {
                        isListening = false
                        onListeningStateChanged(false)
                        val errorDesc = when (errorCode) {
                            SpeechRecognizer.ERROR_AUDIO -> "Errore di registrazione audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Errore client di riconoscimento"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permessi microfono insufficienti"
                            SpeechRecognizer.ERROR_NETWORK -> "Errore di rete"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout di connessione vocale"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Nessuna parola riconosciuta"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Servizio vocale occupato"
                            SpeechRecognizer.ERROR_SERVER -> "Errore server vocale"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nessun parlato rilevato"
                            else -> "Errore vocale sconosciuto ($errorCode)"
                        }
                        onError(errorDesc)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull() ?: ""
                        if (best.isNotBlank()) {
                            onResult(best)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onListeningStateChanged(false)
            onError(e.localizedMessage ?: "Impossibile avviare il microfono")
        }
    }

    fun stopListening() {
        if (isListening || speechRecognizer != null) {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
            isListening = false
        }
    }
}
