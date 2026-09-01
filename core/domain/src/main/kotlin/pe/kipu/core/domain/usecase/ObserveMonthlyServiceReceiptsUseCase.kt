package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.CustomFixedExpenseSerializer
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.refreshTicks
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObserveMonthlyServiceReceiptsUseCase @Inject constructor(
    private val financialPlanRepository: FinancialPlanRepository,
    private val monthlyServiceReceiptRepository: MonthlyServiceReceiptRepository,
    private val movementRepository: MovementRepository,
    private val timeProvider: TimeProvider,
) {
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.of("America/Lima"))

    operator fun invoke(): Flow<List<MonthlyServiceReceipt>> =
        timeProvider.refreshTicks()
            .map(monthFormatter::format)
            .distinctUntilChanged()
            .flatMapLatest { currentMonthKey ->
                combine(
                    financialPlanRepository.observePlans().map { it.firstOrNull() },
                    monthlyServiceReceiptRepository.observeReceiptsForMonth(currentMonthKey),
                    movementRepository.observeMovements(),
                ) { plan, savedReceipts, movements ->
                    if (plan == null) return@combine emptyList<MonthlyServiceReceipt>()

                    val savedByKey = savedReceipts.associateBy { it.key.identifier }
                    val movementAmountsById = movements.associate { it.id to it.amount }
                    val definedServices = buildDefinedReceipts(plan, currentMonthKey)

                    definedServices.map { defined ->
                        val saved = savedByKey[defined.key.identifier]
                        if (saved != null) {
                            defined.copy(
                                isPaid = saved.isPaid,
                                paidMovementId = saved.paidMovementId,
                                paidAt = saved.paidAt,
                                paidAmount = saved.paidMovementId?.let(movementAmountsById::get),
                            )
                        } else {
                            defined
                        }
                    }
                }
            }

    private fun buildDefinedReceipts(plan: FinancialPlan, monthKey: String): List<MonthlyServiceReceipt> {
        val result = mutableListOf<MonthlyServiceReceipt>()

        fun addIfValid(key: ServiceReceiptKey, amount: Money?) {
            if (amount != null && !amount.isZero()) {
                result.add(
                    MonthlyServiceReceipt(
                        key = key,
                        title = key.defaultTitle,
                        configuredAmount = amount,
                        monthKey = monthKey,
                    )
                )
            }
        }

        addIfValid(ServiceReceiptKey.LIGHT, plan.electricityExpenses)
        addIfValid(ServiceReceiptKey.WATER, plan.waterExpenses)
        addIfValid(ServiceReceiptKey.INTERNET, plan.internetExpenses)
        addIfValid(ServiceReceiptKey.PHONE, plan.phoneExpenses)
        addIfValid(ServiceReceiptKey.RENT, plan.rentExpenses)
        addIfValid(ServiceReceiptKey.DEBTS, plan.debtsExpenses)
        addIfValid(ServiceReceiptKey.EDUCATION, plan.educationExpenses)

        val customLines = CustomFixedExpenseSerializer.deserialize(plan.customFixedExpensesJson)
        for (line in customLines) {
            val money = when (val res = pe.kipu.core.domain.util.MoneyInputParser.parsePen(line.amountText)) {
                is pe.kipu.core.domain.model.DomainResult.Ok -> res.value
                is pe.kipu.core.domain.model.DomainResult.Err -> null
            }
            if (money != null && !money.isZero() && line.label.isNotBlank()) {
                val customKey = ServiceReceiptKey.custom(line.label)
                result.add(
                    MonthlyServiceReceipt(
                        key = customKey,
                        title = line.label,
                        configuredAmount = money,
                        monthKey = monthKey,
                    )
                )
            }
        }

        return result
    }
}
