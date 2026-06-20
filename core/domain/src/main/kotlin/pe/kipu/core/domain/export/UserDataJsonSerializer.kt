package pe.kipu.core.domain.export

import javax.inject.Inject
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.UserPreferences

class UserDataJsonSerializer @Inject constructor() {

    fun serialize(snapshot: UserDataSnapshot): String = buildString {
        append('{')
        append("\"exportVersion\":").append(snapshot.exportVersion).append(',')
        append("\"exportedAt\":").append(JsonEscaper.string(snapshot.exportedAt.toString())).append(',')
        append("\"movements\":").append(serializeMovements(snapshot.movements)).append(',')
        append("\"categories\":").append(serializeCategories(snapshot.categories)).append(',')
        append("\"envelopes\":").append(serializeEnvelopes(snapshot.envelopes)).append(',')
        append("\"commitments\":").append(serializeCommitments(snapshot.commitments)).append(',')
        append("\"financialPlans\":").append(serializeFinancialPlans(snapshot.financialPlans)).append(',')
        append("\"gatherings\":").append(serializeGatherings(snapshot.gatherings)).append(',')
        append("\"dismissedDuplicatePairKeys\":").append(serializeStringSet(snapshot.dismissedDuplicatePairKeys)).append(',')
        append("\"preferences\":").append(serializePreferences(snapshot.preferences))
        append('}')
    }

    private fun serializeMovements(movements: List<Movement>): String = buildString {
        append('[')
        movements.forEachIndexed { index, movement ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(movement.id)).append(',')
            append("\"type\":").append(JsonEscaper.string(movement.type.name)).append(',')
            append("\"amount\":").append(JsonEscaper.string(movement.amount.amount.toPlainString())).append(',')
            append("\"categoryId\":").append(JsonEscaper.string(movement.categoryId)).append(',')
            append("\"channel\":").append(JsonEscaper.string(movement.channel.name)).append(',')
            append("\"source\":").append(JsonEscaper.string(movement.source.name)).append(',')
            append("\"status\":").append(JsonEscaper.string(movement.status.name)).append(',')
            append("\"description\":").append(JsonEscaper.string(movement.description)).append(',')
            append("\"counterpartyName\":").append(JsonEscaper.string(movement.counterpartyName)).append(',')
            append("\"operationNumber\":").append(JsonEscaper.string(movement.operationNumber)).append(',')
            append("\"recordedAt\":").append(JsonEscaper.string(movement.recordedAt.toString())).append(',')
            append("\"createdAt\":").append(JsonEscaper.string(movement.createdAt.toString()))
            append('}')
        }
        append(']')
    }

    private fun serializeCategories(categories: List<Category>): String = buildString {
        append('[')
        categories.forEachIndexed { index, category ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(category.id)).append(',')
            append("\"name\":").append(JsonEscaper.string(category.name)).append(',')
            append("\"iconKey\":").append(JsonEscaper.string(category.iconKey))
            append('}')
        }
        append(']')
    }

    private fun serializeEnvelopes(envelopes: List<Envelope>): String = buildString {
        append('[')
        envelopes.forEachIndexed { index, envelope ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(envelope.id)).append(',')
            append("\"name\":").append(JsonEscaper.string(envelope.name)).append(',')
            append("\"weeklyLimit\":").append(JsonEscaper.string(envelope.weeklyLimit.amount.toPlainString())).append(',')
            append("\"categoryId\":").append(JsonEscaper.string(envelope.categoryId))
            append('}')
        }
        append(']')
    }

    private fun serializeCommitments(commitments: List<Commitment>): String = buildString {
        append('[')
        commitments.forEachIndexed { index, commitment ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(commitment.id)).append(',')
            append("\"type\":").append(JsonEscaper.string(commitment.type.name)).append(',')
            append("\"title\":").append(JsonEscaper.string(commitment.title)).append(',')
            append("\"targetAmount\":").append(JsonEscaper.string(commitment.targetAmount?.amount?.toPlainString())).append(',')
            append("\"currentAmount\":").append(JsonEscaper.string(commitment.currentAmount?.amount?.toPlainString())).append(',')
            append("\"dueDate\":").append(JsonEscaper.string(commitment.dueDate?.toString())).append(',')
            append("\"counterpartyName\":").append(JsonEscaper.string(commitment.counterpartyName)).append(',')
            append("\"isSettled\":").append(commitment.isSettled)
            append('}')
        }
        append(']')
    }

    private fun serializeFinancialPlans(plans: List<FinancialPlan>): String = buildString {
        append('[')
        plans.forEachIndexed { index, plan ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(plan.id)).append(',')
            append("\"estimatedMonthlyIncome\":").append(JsonEscaper.string(plan.estimatedMonthlyIncome.amount.toPlainString())).append(',')
            append("\"fixedExpenses\":").append(JsonEscaper.string(plan.fixedExpenses.amount.toPlainString())).append(',')
            append("\"envelopeIds\":").append(serializeStringList(plan.envelopeIds))
            append('}')
        }
        append(']')
    }

    private fun serializeGatherings(gatherings: List<Gathering>): String = buildString {
        append('[')
        gatherings.forEachIndexed { index, gathering ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(gathering.id)).append(',')
            append("\"name\":").append(JsonEscaper.string(gathering.name)).append(',')
            append("\"participantCount\":").append(gathering.participantCount).append(',')
            append("\"participantNames\":").append(serializeStringList(gathering.participantNames))
            append('}')
        }
        append(']')
    }

    private fun serializePreferences(preferences: UserPreferences): String = buildString {
        append('{')
        append("\"themeMode\":").append(JsonEscaper.string(preferences.themeMode.name)).append(',')
        append("\"notificationsEnabled\":").append(preferences.notificationsEnabled).append(',')
        append("\"onboardingCompleted\":").append(preferences.onboardingCompleted).append(',')
        append("\"pendingPlanWizard\":").append(preferences.pendingPlanWizard).append(',')
        append("\"antSpendingWeeklyLimitCents\":").append(preferences.antSpendingWeeklyLimitCents ?: "null").append(',')
        append("\"antSpendingAlertEnabled\":").append(preferences.antSpendingAlertEnabled).append(',')
        append("\"antSpendingAlertPercent\":").append(preferences.antSpendingAlertPercent).append(',')
        append("\"antSpendingTrackedCategories\":").append(serializeStringSet(preferences.antSpendingTrackedCategories)).append(',')
        append("\"widgetDailyAvailableText\":").append(JsonEscaper.string(preferences.widgetDailyAvailableText)).append(',')
        append("\"widgetIsOverBudget\":").append(preferences.widgetIsOverBudget)
        append('}')
    }

    private fun serializeStringList(values: List<String>): String = buildString {
        append('[')
        values.forEachIndexed { index, value ->
            if (index > 0) append(',')
            append(JsonEscaper.string(value))
        }
        append(']')
    }

    private fun serializeStringSet(values: Set<String>): String = serializeStringList(values.toList())
}
