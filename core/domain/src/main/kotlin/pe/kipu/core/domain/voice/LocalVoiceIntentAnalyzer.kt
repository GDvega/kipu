package pe.kipu.core.domain.voice

import javax.inject.Inject

class LocalVoiceIntentAnalyzer @Inject constructor(
    private val parser: VoiceFinancialIntentParser,
) : VoiceIntentAnalyzer {

    constructor() : this(VoiceFinancialIntentParser())

    override suspend fun analyze(rawText: String): VoiceFinancialIntent {
        return parser.parse(rawText)
    }
}
