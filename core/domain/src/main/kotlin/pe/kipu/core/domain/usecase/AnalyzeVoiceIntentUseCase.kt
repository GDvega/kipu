package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.voice.VoiceFinancialIntent
import pe.kipu.core.domain.voice.VoiceIntentAnalyzer

class AnalyzeVoiceIntentUseCase @Inject constructor(
    private val voiceIntentAnalyzer: VoiceIntentAnalyzer,
) {

    suspend operator fun invoke(rawText: String): VoiceFinancialIntent {
        return voiceIntentAnalyzer.analyze(rawText)
    }
}
