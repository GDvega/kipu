package pe.kipu.feature.plan.presentation

enum class PlanWizardStep {
    Income,
    FixedExpenses,
    Envelopes,
    AntSpending,
    Goal,
    Summary,
}

fun PlanWizardStep.stepIndex(): Int = when (this) {
    PlanWizardStep.Income -> 1
    PlanWizardStep.FixedExpenses -> 2
    PlanWizardStep.Envelopes -> 3
    PlanWizardStep.AntSpending -> 4
    PlanWizardStep.Goal -> 5
    PlanWizardStep.Summary -> 6
}

const val PLAN_WIZARD_TOTAL_STEPS: Int = 6

fun planWizardStepFromRoute(value: String?): PlanWizardStep = when (value) {
    "expenses" -> PlanWizardStep.FixedExpenses
    "envelopes" -> PlanWizardStep.Envelopes
    "ant" -> PlanWizardStep.AntSpending
    "goal" -> PlanWizardStep.Goal
    "summary" -> PlanWizardStep.Summary
    else -> PlanWizardStep.Income
}

fun PlanWizardStep.routeValue(): String = when (this) {
    PlanWizardStep.Income -> "income"
    PlanWizardStep.FixedExpenses -> "expenses"
    PlanWizardStep.Envelopes -> "envelopes"
    PlanWizardStep.AntSpending -> "ant"
    PlanWizardStep.Goal -> "goal"
    PlanWizardStep.Summary -> "summary"
}
