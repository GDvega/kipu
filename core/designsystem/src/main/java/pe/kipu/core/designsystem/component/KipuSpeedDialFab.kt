package pe.kipu.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class SpeedDialAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun KipuSpeedDialFab(
    actions: List<SpeedDialAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    modalBottomPadding: Dp = 0.dp,
) {
    if (expanded) {
        Dialog(
            onDismissRequest = { onExpandedChange(false) },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                windowTitle = "Menú de registro",
            ),
        ) {
            SpeedDialContent(
                actions = actions,
                expanded = true,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.fillMaxSize(),
                bottomPadding = modalBottomPadding,
            )
        }
    } else {
        SpeedDialContent(
            actions = actions,
            expanded = false,
            onExpandedChange = onExpandedChange,
            modifier = modifier,
        )
    }
}

@Composable
private fun SpeedDialContent(
    actions: List<SpeedDialAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f)

    Box(modifier = modifier) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(onExpandedChange) {
                        detectTapGestures { onExpandedChange(false) }
                    }
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + bottomPadding,
                )
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    actions.forEach { action ->
                        val invokeAction = {
                            onExpandedChange(false)
                            action.onClick()
                        }
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = invokeAction,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = action.label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                shadowElevation = 6.dp,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(action.icon, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier
                    .testTag(KipuTestTags.REGISTER_FAB)
                    .semantics {
                        contentDescription = if (expanded) {
                            "Cerrar menú de registro"
                        } else {
                            "Abrir menú de registro"
                        }
                    },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}
