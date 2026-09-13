package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.feature.home.ui.MonthlyReceiptsCard

class FinancialRemediationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun receiptShowsActualPaymentAndReturnsToReferenceWhenPaymentIsRemoved() {
        val payment = MonthlyServiceReceipt(
            key = ServiceReceiptKey.LIGHT, title = "Luz", configuredAmount = money("45"), monthKey = "2026-09",
            isPaid = true, paidMovementId = "payment", paidAmount = money("55"),
        )
        val receipt = mutableStateOf(payment)
        composeRule.setContent {
            KipuTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MonthlyReceiptsCard(listOf(receipt.value), onMarkReceiptPaid = { _, _ -> }, onUnmarkReceiptPaid = {})
                }
            }
        }
        composeRule.onNodeWithText("${format("55")} pagados · Plan ${format("45")}").assertIsDisplayed()
        composeRule.onNodeWithText("Pagos mensuales").performClick()
        composeRule.onNodeWithText("Pagaste ${format("55")}").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            receipt.value = payment.copy(isPaid = false, paidMovementId = null, paidAmount = null)
        }
        composeRule.onNodeWithText("Pagaste ${format("55")}").assertDoesNotExist()
        composeRule.onNodeWithText("Referencia ${format("45")}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0/1 pagados").performScrollTo().assertIsDisplayed()
    }

    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()
    private fun format(value: String) = formatPenAmountForDisplay(money(value).amount)
}
