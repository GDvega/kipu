package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.feature.movements.presentation.ManualMovementChannelOption
import pe.kipu.feature.movements.ui.ManualMovementDialog
import pe.kipu.feature.movements.ui.ManualMovementFormState

@RunWith(AndroidJUnit4::class)
class HighFormsAccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun amountErrorIsRenderedOnceAndAnnouncedFromItsField() {
        val message = "Ingresa un monto"
        composeRule.setContent {
            KipuTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ManualMovementDialog(
                        categories = emptyList(),
                        formState = ManualMovementFormState(amountErrorMessage = message),
                        onMovementTypeSelected = {},
                        onChannelSelected = { _: ManualMovementChannelOption -> },
                        onAmountChanged = {},
                        onCategorySelected = {},
                        onDescriptionChanged = {},
                        onCounterpartyChanged = {},
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onAllNodesWithText(message).assertCountEquals(1)
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, message),
        ).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
    }
}
