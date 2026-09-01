package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.feature.home.ui.MonthlyReceiptsCard

@RunWith(AndroidJUnit4::class)
class MonthlyServicePaymentUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun paymentDialogSavesActualAmountAndKeepsPlanReference() {
        val reference = Money.of(BigDecimal("45.00")).getOrError()
        var savedAmount: Money? = null
        composeRule.setContent {
            KipuTheme {
                MonthlyReceiptsCard(
                    receipts = listOf(
                        MonthlyServiceReceipt(
                            key = ServiceReceiptKey.LIGHT,
                            title = "Luz",
                            configuredAmount = reference,
                            monthKey = "2026-08",
                        ),
                    ),
                    onMarkReceiptPaid = { _, amount -> savedAmount = amount },
                    onUnmarkReceiptPaid = {},
                )
            }
        }

        composeRule.onNode(
            hasClickAction() and hasText("Pagos mensuales"),
        ).performClick()
        composeRule.onNodeWithText("Pagar").performClick()
        composeRule.onNodeWithText("Referencia del plan: S/ 45.00", substring = true).assertExists()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("55")
        composeRule.onNodeWithText("Registrar pago").performClick()

        composeRule.runOnIdle {
            assertEquals(Money.of(BigDecimal("55.00")).getOrError(), savedAmount)
        }
    }
}
