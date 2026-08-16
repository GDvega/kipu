package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.feature.plan.presentation.PlanWizardStep
import pe.kipu.feature.plan.presentation.PlanWizardUiState
import pe.kipu.feature.plan.ui.AntSpendingStepContent
import pe.kipu.feature.plan.ui.GoalStepContent

@RunWith(AndroidJUnit4::class)
class MediumAccessibilitySemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun amountPresetsBelongToASelectableGroup() {
        composeRule.setContent {
            KipuTheme {
                AntSpendingStepContent(
                    budgetCycle = BudgetCycle.WEEKLY,
                    categories = emptyList(),
                    limitText = "35",
                    selectedCategoryIds = emptySet(),
                    pendingCategoryName = "",
                    alertEnabled = false,
                    onLimitChanged = {},
                    onPresetSelected = {},
                    onCategoryToggled = {},
                    onPendingCategoryNameChanged = {},
                    onAddAntCategory = {},
                    onQuickAntCategorySelected = {},
                    onAlertToggled = {},
                )
            }
        }

        val selectableGroup = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        composeRule.onNode(hasText("S/ 25") and hasAnyAncestor(selectableGroup)).assertExists()
    }

    @Test
    fun antiSpendingAlertExposesOneLabeledSwitchTarget() {
        var enabled = false
        composeRule.setContent {
            KipuTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    AntSpendingStepContent(
                        budgetCycle = BudgetCycle.WEEKLY,
                        categories = emptyList(),
                        limitText = "50",
                        selectedCategoryIds = emptySet(),
                        pendingCategoryName = "",
                        alertEnabled = enabled,
                        onLimitChanged = {},
                        onPresetSelected = {},
                        onCategoryToggled = {},
                        onPendingCategoryNameChanged = {},
                        onAddAntCategory = {},
                        onQuickAntCategorySelected = {},
                        onAlertToggled = { enabled = it },
                    )
                }
            }
        }

        composeRule.onNode(hasText("Avisarme al 80%") and hasClickAction())
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assert(switchRole)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .performClick()

        composeRule.runOnIdle { assertTrue(enabled) }
    }

    @Test
    fun socialDebtExposesOneLabeledSwitchTarget() {
        var enabled = false
        composeRule.setContent {
            KipuTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    GoalStepContent(
                        state = PlanWizardUiState.Content(step = PlanWizardStep.Goal),
                        onGoalTypeSelected = {},
                        onGoalNameChanged = {},
                        onGoalTargetChanged = {},
                        onGoalCurrentChanged = {},
                        onGoalMonthsChanged = {},
                        onSocialDebtToggled = { enabled = it },
                        onSocialDebtCounterpartyChanged = {},
                        onSocialDebtAmountChanged = {},
                        onSkip = {},
                    )
                }
            }
        }

        composeRule.onNode(hasText("¿Le debes a alguien?") and hasClickAction())
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assert(switchRole)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .performClick()

        composeRule.runOnIdle { assertTrue(enabled) }
    }

    private val switchRole = SemanticsMatcher.expectValue(
        SemanticsProperties.Role,
        Role.Switch,
    )
}
