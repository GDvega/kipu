package pe.kipu.core.domain.export

import javax.inject.Inject
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
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
        append("\"gatheringExpenses\":").append(serializeGatheringExpenses(snapshot.gatheringExpenses)).append(',')
        append("\"dismissedDuplicatePairKeys\":").append(serializeStringSet(snapshot.dismissedDuplicatePairKeys)).append(',')
        append("\"monthlyServiceReceipts\":").append(serializeMonthlyServiceReceipts(snapshot.monthlyServiceReceipts)).append(',')
        append("\"movementAuditEntries\":").append(serializeMovementAuditEntries(snapshot.movementAuditEntries)).append(',')
        append("\"reserveEvents\":").append(serializeReserveEvents(snapshot.reserveEvents)).append(',')
        append("\"preferences\":").append(serializePreferences(snapshot.preferences))
        append('}')
    }

    private fun serializeMonthlyServiceReceipts(receipts: List<MonthlyServiceReceipt>): String = buildString {
        append('[')
        receipts.forEachIndexed { index, receipt ->
            if (index > 0) append(',')
            append('{')
            append("\"key\":").append(JsonEscaper.string(receipt.key.identifier)).append(',')
            append("\"title\":").append(JsonEscaper.string(receipt.title)).append(',')
            append("\"configuredAmount\":").append(JsonEscaper.string(receipt.configuredAmount.amount.toPlainString())).append(',')
            append("\"monthKey\":").append(JsonEscaper.string(receipt.monthKey)).append(',')
            append("\"isPaid\":").append(receipt.isPaid).append(',')
            append("\"paidMovementId\":").append(JsonEscaper.string(receipt.paidMovementId)).append(',')
            append("\"paidAt\":").append(JsonEscaper.string(receipt.paidAt?.toString()))
            append(',')
            append("\"paidAmount\":").append(JsonEscaper.string(receipt.paidAmount?.amount?.toPlainString()))
            append('}')
        }
        append(']')
    }

    private fun serializeReserveEvents(events: List<ReserveEvent>): String = buildString {
        append('[')
        events.forEachIndexed { index, event ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(event.id)).append(',')
            append("\"type\":").append(JsonEscaper.string(event.type.name)).append(',')
            append("\"amount\":").append(JsonEscaper.string(event.amount.amount.toPlainString())).append(',')
            append("\"sourceMovementId\":").append(JsonEscaper.string(event.sourceMovementId)).append(',')
            append("\"reversesEventId\":").append(JsonEscaper.string(event.reversesEventId)).append(',')
            append("\"occurredAt\":").append(JsonEscaper.string(event.occurredAt.toString())).append(',')
            append("\"createdAt\":").append(JsonEscaper.string(event.createdAt.toString()))
            append('}')
        }
        append(']')
    }

    private fun serializeMovementAuditEntries(entries: List<MovementAuditEntry>): String = buildString {
        append('[')
        entries.forEachIndexed { index, entry ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(entry.id)).append(',')
            append("\"movementId\":").append(JsonEscaper.string(entry.movementId)).append(',')
            append("\"action\":").append(JsonEscaper.string(entry.action.name)).append(',')
            append("\"movementType\":").append(JsonEscaper.string(entry.movementType.name)).append(',')
            append("\"amount\":").append(JsonEscaper.string(entry.amount.amount.toPlainString())).append(',')
            append("\"categoryId\":").append(JsonEscaper.string(entry.categoryId)).append(',')
            append("\"categoryName\":").append(JsonEscaper.string(entry.categoryName)).append(',')
            append("\"channel\":").append(JsonEscaper.string(entry.channel.name)).append(',')
            append("\"description\":").append(JsonEscaper.string(entry.description)).append(',')
            append("\"counterpartyName\":").append(JsonEscaper.string(entry.counterpartyName)).append(',')
            append("\"details\":").append(JsonEscaper.string(entry.details)).append(',')
            append("\"timestamp\":").append(JsonEscaper.string(entry.timestamp.toString()))
            append('}')
        }
        append(']')
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
            append("\"commitmentId\":").append(JsonEscaper.string(movement.commitmentId)).append(',')
            append("\"envelopeId\":").append(JsonEscaper.string(movement.envelopeId)).append(',')
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
            append("\"isSettled\":").append(commitment.isSettled).append(',')
            append("\"currencyCode\":").append(JsonEscaper.string(commitment.currencyCode)).append(',')
            append("\"savingsHorizonMonths\":").append(commitment.savingsHorizonMonths?.toString() ?: "null")
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
            append("\"initialBalance\":").append(JsonEscaper.string(plan.initialBalance.amount.toPlainString())).append(',')
            append("\"reserveMonthlyContribution\":").append(JsonEscaper.string(plan.reserveMonthlyContribution.amount.toPlainString())).append(',')
            append("\"envelopeIds\":").append(serializeStringList(plan.envelopeIds)).append(',')
            append("\"incomeProfile\":").append(JsonEscaper.string(plan.incomeProfile.name)).append(',')
            append("\"payFrequency\":").append(JsonEscaper.string(plan.payFrequency.name)).append(',')
            append("\"budgetCycle\":").append(JsonEscaper.string(plan.budgetCycle.name)).append(',')
            append("\"antSpendingLimit\":").append(JsonEscaper.string(plan.antSpendingLimit?.amount?.toPlainString())).append(',')
            append("\"antSpendingAlertEnabled\":").append(plan.antSpendingAlertEnabled).append(',')
            append("\"antSpendingAlertPercent\":").append(plan.antSpendingAlertPercent).append(',')
            append("\"antSpendingTrackedCategoryIds\":").append(serializeStringSet(plan.antSpendingTrackedCategoryIds)).append(',')
            append("\"electricityExpenses\":").append(JsonEscaper.string(plan.electricityExpenses?.amount?.toPlainString())).append(',')
            append("\"waterExpenses\":").append(JsonEscaper.string(plan.waterExpenses?.amount?.toPlainString())).append(',')
            append("\"internetExpenses\":").append(JsonEscaper.string(plan.internetExpenses?.amount?.toPlainString())).append(',')
            append("\"rentExpenses\":").append(JsonEscaper.string(plan.rentExpenses?.amount?.toPlainString())).append(',')
            append("\"phoneExpenses\":").append(JsonEscaper.string(plan.phoneExpenses?.amount?.toPlainString())).append(',')
            append("\"debtsExpenses\":").append(JsonEscaper.string(plan.debtsExpenses?.amount?.toPlainString())).append(',')
            append("\"educationExpenses\":").append(JsonEscaper.string(plan.educationExpenses?.amount?.toPlainString())).append(',')
            append("\"customFixedExpensesJson\":").append(JsonEscaper.string(plan.customFixedExpensesJson))
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
            append("\"participantNames\":").append(serializeStringList(gathering.participantNames)).append(',')
            append("\"isSettled\":").append(gathering.isSettled)
            append('}')
        }
        append(']')
    }

    private fun serializeGatheringExpenses(expenses: List<GatheringExpense>): String = buildString {
        append('[')
        expenses.forEachIndexed { index, expense ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(JsonEscaper.string(expense.id)).append(',')
            append("\"gatheringId\":").append(JsonEscaper.string(expense.gatheringId)).append(',')
            append("\"amount\":").append(JsonEscaper.string(expense.amount.amount.toPlainString())).append(',')
            append("\"paidByParticipant\":").append(JsonEscaper.string(expense.paidByParticipant)).append(',')
            append("\"description\":").append(JsonEscaper.string(expense.description)).append(',')
            append("\"movementId\":").append(JsonEscaper.string(expense.movementId)).append(',')
            append("\"recordedAt\":").append(JsonEscaper.string(expense.recordedAt.toString()))
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
        append("\"widgetIsOverBudget\":").append(preferences.widgetIsOverBudget).append(',')
        append("\"widgetDailyAvailableUpdatedAtMillis\":").append(preferences.widgetDailyAvailableUpdatedAtMillis ?: "null").append(',')
        append("\"budgetCycle\":").append(JsonEscaper.string(preferences.budgetCycle.name))
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
