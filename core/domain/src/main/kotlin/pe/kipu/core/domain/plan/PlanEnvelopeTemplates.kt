package pe.kipu.core.domain.plan

import java.math.BigDecimal

import pe.kipu.core.domain.model.BudgetCycle

data class PlanEnvelopeTemplate(
    val envelopeId: String,
    val name: String,
    val subtitle: String,
    val presetAmounts: List<BigDecimal>,
    val defaultPresetIndex: Int = 1,
)

object PlanEnvelopeTemplates {
    const val CUSTOM_ENVELOPE_PREFIX = "envelope-plan-"

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

    fun presetsForCycle(template: PlanEnvelopeTemplate, cycle: BudgetCycle): List<BigDecimal> = when (cycle) {
        BudgetCycle.DAILY -> when (template.envelopeId) {
            DefaultPlanEnvelopeIds.FOOD -> listOf(BigDecimal("15"), BigDecimal("25"), BigDecimal("35"))
            DefaultPlanEnvelopeIds.TRANSPORT -> listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("15"))
            DefaultPlanEnvelopeIds.LEISURE -> listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("20"))
            DefaultPlanEnvelopeIds.FAMILY -> listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("20"))
            else -> listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("20"))
        }
        BudgetCycle.WEEKLY -> template.presetAmounts
        BudgetCycle.MONTHLY -> when (template.envelopeId) {
            DefaultPlanEnvelopeIds.FOOD -> listOf(BigDecimal("350"), BigDecimal("500"), BigDecimal("750"))
            DefaultPlanEnvelopeIds.TRANSPORT -> listOf(BigDecimal("120"), BigDecimal("200"), BigDecimal("350"))
            DefaultPlanEnvelopeIds.LEISURE -> listOf(BigDecimal("150"), BigDecimal("300"), BigDecimal("500"))
            DefaultPlanEnvelopeIds.FAMILY -> listOf(BigDecimal("200"), BigDecimal("400"), BigDecimal("600"))
            else -> listOf(BigDecimal("100"), BigDecimal("250"), BigDecimal("500"))
        }
    }

    fun antSpendingPresetsForCycle(cycle: BudgetCycle): List<BigDecimal> = when (cycle) {
        BudgetCycle.DAILY -> listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("15"))
        BudgetCycle.WEEKLY -> listOf(BigDecimal("25"), BigDecimal("35"), BigDecimal("50"))
        BudgetCycle.MONTHLY -> listOf(BigDecimal("100"), BigDecimal("150"), BigDecimal("200"))
    }

    fun defaultWeeklyLimit(template: PlanEnvelopeTemplate): BigDecimal =
        template.presetAmounts.getOrElse(template.defaultPresetIndex) {
            template.presetAmounts.first()
        }

    fun wizardEnvelopeIds(): List<String> = buildList {
        WIZARD_ENVELOPES.forEach { add(it.envelopeId) }
        add(ANT_SPENDING_ENVELOPE_ID)
    }

    fun wizardEnvelopeIdsCsv(): String = wizardEnvelopeIds().joinToString(",")

    fun isWizardManagedCustomEnvelope(envelopeId: String): Boolean =
        envelopeId.startsWith(CUSTOM_ENVELOPE_PREFIX)
}

object DefaultPlanEnvelopeIds {
    const val FOOD = "envelope-food"
    const val TRANSPORT = "envelope-transport"
    const val SERVICES = "envelope-services"
    const val LEISURE = "envelope-leisure"
    const val FAMILY = "envelope-family"
    const val ANT_SPENDING = "envelope-ant-spending"
}
