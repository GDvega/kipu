package pe.kipu.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/** Shared layout tokens for screen content alignment. */
object KipuLayout {
    val screenHorizontalPadding = 24.dp
    val listItemSpacing = 12.dp
    val sectionSpacing = 16.dp

    fun screenContentPadding(bottom: Int = 24): PaddingValues = PaddingValues(
        start = screenHorizontalPadding,
        end = screenHorizontalPadding,
        bottom = bottom.dp,
    )

    fun listContentPadding(fabClearance: Boolean = false): PaddingValues = PaddingValues(
        start = screenHorizontalPadding,
        end = screenHorizontalPadding,
        top = 0.dp,
        bottom = if (fabClearance) 88.dp else screenHorizontalPadding,
    )
}
