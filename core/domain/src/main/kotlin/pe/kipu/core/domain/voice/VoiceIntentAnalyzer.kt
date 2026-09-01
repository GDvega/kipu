package pe.kipu.core.domain.voice

interface VoiceIntentAnalyzer {
    suspend fun analyze(rawText: String): VoiceFinancialIntent
}
