package pe.kipu.core.designsystem.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface VoiceSpeechState {
    data object Idle : VoiceSpeechState
    data class Listening(val rmsDb: Float = 0f, val partialText: String = "") : VoiceSpeechState
    data object Processing : VoiceSpeechState
    data class Success(val recognizedText: String) : VoiceSpeechState
    data class Error(val message: String) : VoiceSpeechState
}

class VoiceSpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<VoiceSpeechState>(VoiceSpeechState.Idle)
    val state: StateFlow<VoiceSpeechState> = _state.asStateFlow()

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoiceSpeechState.Error("El reconocimiento de voz no está disponible en este dispositivo")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceSpeechState.Listening()
                }

                override fun onBeginningOfSpeech() {
                    _state.value = VoiceSpeechState.Listening()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val current = _state.value
                    if (current is VoiceSpeechState.Listening) {
                        _state.value = current.copy(rmsDb = rmsdB.coerceAtLeast(0f))
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _state.value = VoiceSpeechState.Processing
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Error al capturar audio"
                        SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de voz"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no otorgado"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Error de red en reconocimiento"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No entendimos lo que dijiste. Intenta de nuevo."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor de voz ocupado"
                        SpeechRecognizer.ERROR_SERVER -> "Error en el servicio de voz"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No escuchamos nada. Habla más cerca."
                        else -> "No pudimos escuchar. Intenta de nuevo."
                    }
                    _state.value = VoiceSpeechState.Error(message)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        _state.value = VoiceFinancialIntentStateSuccess(text)
                    } else {
                        _state.value = VoiceSpeechState.Error("No se detectó ningún comando")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty()
                    val current = _state.value
                    if (current is VoiceSpeechState.Listening) {
                        _state.value = current.copy(partialText = partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
		} catch (_: Exception) {
			_state.value = VoiceSpeechState.Error("No se pudo iniciar el micrófono")
        }
    }

	fun onPermissionDenied() {
		_state.value = VoiceSpeechState.Error("Necesitamos permiso de micrófono para escuchar el comando")
	}

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun reset() {
        stopListening()
        _state.value = VoiceSpeechState.Idle
    }

    private fun VoiceFinancialIntentStateSuccess(text: String): VoiceSpeechState =
        VoiceSpeechState.Success(text)
}
