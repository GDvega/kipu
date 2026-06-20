package pe.kipu.core.domain.plan

import java.math.BigDecimal

data class PlanEnvelopeTemplate(
    val envelopeId: String,
    val name: String,
    val subtitle: String,
    val presetAmounts: List<BigDecimal>,
    val defaultPresetIndex: Int = 1,
)

object PlanEnvelopeTemplates {
    val WIZARD_ENVELOPES: List<PlanEnvelopeTemplate> = listOf(
        PlanEnvelopeTemplate(
            envelopeId = DefaultPlanEnvelopeIds.FOOD,
            name = "Comida",
            subtitle = "Menú, delivery, snacks",
            presetAmounts = listOf(
                BigDecimal("80"),
                BigDecimal("120"),
                BigDecimal("180"),
            ),
        ),
        PlanEnvelopeTemplate(
            envelopeId = DefaultPlanEnvelopeIds.TRANSPORT,
            name = "Transporte",
            subtitle = "Pasajes, taxi, movilidad",
            presetAmounts = listOf(
                BigDecimal("30"),
                BigDecimal("50"),
                BigDecimal("80"),
            ),
        ),
        PlanEnvelopeTemplate(
            envelopeId = DefaultPlanEnvelopeIds.LEISURE,
            name = "Ocio",
            subtitle = "Salidas, entretenimiento",
            presetAmounts = listOf(
                BigDecimal("40"),
                BigDecimal("80"),
                BigDecimal("120"),
            ),
        ),
        PlanEnvelopeTemplate(
            envelopeId = DefaultPlanEnvelopeIds.FAMILY,
            name = "Familia",
            subtitle = "Apoyo, regalos, emergencias",
            presetAmounts = listOf(
                BigDecimal("50"),
                BigDecimal("100"),
                BigDecimal("150"),
            ),
        ),
    )

    val ANT_SPENDING_ENVELOPE_ID: String = DefaultPlanEnvelopeIds.ANT_SPENDING

    val ANT_SPENDING_PRESETS: List<BigDecimal> = listOf(
        BigDecimal("25"),
        BigDecimal("35"),
        BigDecimal("50"),
    )

    fun defaultWeeklyLimit(template: PlanEnvelopeTemplate): BigDecimal =
        template.presetAmounts.getOrElse(template.defaultPresetIndex) {
            template.presetAmounts.first()
        }
}

object DefaultPlanEnvelopeIds {
    const val FOOD = "envelope-food"
    const val TRANSPORT = "envelope-transport"
    const val SERVICES = "envelope-services"
    const val LEISURE = "envelope-leisure"
    const val FAMILY = "envelope-family"
    const val ANT_SPENDING = "envelope-ant-spending"
}
