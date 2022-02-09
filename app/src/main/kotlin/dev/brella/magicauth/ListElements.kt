package dev.brella.magicauth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
@ExperimentalMaterialApi
fun DismissableAccount(
    state: DismissState,
    modifier: Modifier = Modifier,
    directions: Set<DismissDirection> = setOf(DismissDirection.EndToStart),
    dismissThresholds: (DismissDirection) -> ThresholdConfig = { FractionalThreshold(0.5f) },

    cardModifier: Modifier = Modifier.fillMaxSize(),
    cardShape: Shape = MaterialTheme.shapes.small,
    cardBackgroundColor: Color = MaterialTheme.colors.surface,
    cardContentColor: Color = contentColorFor(cardBackgroundColor),
    cardBorder: BorderStroke? = null,
    cardElevation: Dp = 1.dp,

    content: @Composable () -> Unit
) = SwipeToDismiss(
    state = state,
    modifier = modifier,
    directions = directions,
    dismissThresholds = dismissThresholds,
    background = {
        if (state.dismissDirection != DismissDirection.EndToStart) return@SwipeToDismiss

        val color by animateColorAsState(
            when (state.targetValue) {
                DismissValue.Default -> MaterialTheme.colors.surface.copy(
                    alpha = DrawerDefaults.ScrimOpacity
                )
                DismissValue.DismissedToEnd -> MaterialTheme.colors.primary.copy(
                    alpha = DrawerDefaults.ScrimOpacity
                )
                DismissValue.DismissedToStart -> MaterialTheme.colors.error
            }
        )
        val tint by animateColorAsState(
            when (state.targetValue) {
                DismissValue.Default -> MaterialTheme.colors.onSurface
                DismissValue.DismissedToEnd -> MaterialTheme.colors.onPrimary
                DismissValue.DismissedToStart -> LocalContentColor.current.copy(
                    alpha = LocalContentAlpha.current
                )
            }
        )
        val alignment = Alignment.CenterEnd
        val icon = Icons.Default.Delete
        val scale by animateFloatAsState(
            if (state.targetValue == DismissValue.Default) 0.75f else 1f
        )

        Card(
            modifier = cardModifier,
            shape = cardShape,
            backgroundColor = cardBackgroundColor,
            contentColor = cardContentColor,
            border = cardBorder,
            elevation = cardElevation,
        ) {
            Box(
                Modifier
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    icon,
                    contentDescription = "Delete Account",
                    modifier = Modifier.scale(scale),
                    tint = tint
                )
            }
        }
    }
) {
    //This is currently disabled due to a bug with animations and alerts
//    val elevation = animateDpAsState(if (state.dismissDirection != null) 4.dp else 0.dp)

    Card(
        modifier = cardModifier,
        shape = cardShape,
        backgroundColor = cardBackgroundColor,
        contentColor = cardContentColor,
        border = cardBorder,
        elevation = 4.dp,
        content = content
    )
}

@Composable
inline fun DeleteAccountDialog(
    account: TOTPAccount,
    noinline onDismiss: () -> Unit,
    noinline onDelete: () -> Unit
) =
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete ${account.issuer ?: "(Unknown Issuer)"} Token?") },
        text = { Text("Do you want to delete ${account.accountName} on ${account.issuer ?: "(Unknown Issuer)"}?\nThis token will be completely and irreversibly deleted in 48 hours") },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = onDismiss) {
                    Text("Dismiss")
                }

                Spacer(modifier = Modifier.width(32.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                ) {
                    Text("Delete")
                }
            }
        }
    )