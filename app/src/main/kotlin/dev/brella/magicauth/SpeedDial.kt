package dev.brella.magicauth

import android.graphics.Point
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout

/**
 * <a href="https://material.io/components/buttons-floating-action-button" class="external" target="_blank">Material Design floating action button</a>.
 *
 * A floating action button (FAB) represents the primary action of a screen.
 *
 * ![Floating action button image](https://developer.android.com/images/reference/androidx/compose/material/floating-action-button.png)
 *
 * This FAB is typically used with an [Icon]:
 *
 * @sample androidx.compose.material.samples.SimpleFab
 *
 * See [ExtendedFloatingActionButton] for an extended FAB that contains text and an optional icon.
 *
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 * when it is null.
 * @param modifier [Modifier] to be applied to this FAB.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB.
 * @param content the content of this FAB - this is typically an [Icon].
 */
@OptIn(ExperimentalMaterialApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun SpeedDial(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    distance: Dp = 128.dp,
    anglePer: Float = 45f,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 500),
    openButton: @Composable () -> Unit,
    closeButton: @Composable () -> Unit,
    content: MutableList<SpeedDialButton>.() -> Unit
) {
    ConstraintLayout {
        val buttons = remember { ArrayList<SpeedDialButton>().apply(content) }
        var opened: Boolean by remember { mutableStateOf(false) }
        val maxAngle: Float by animateFloatAsState(
            if (opened) buttons.size * anglePer else 0f,
            animationSpec = animationSpec
        )

        buttons.forEachIndexed { i, button ->
            val buttonAngle = ((anglePer + anglePer * i) % 360f)
            FloatingActionButton(
                button.onClick,
                button
                    .modifier(modifier)
                    .constrainAs(createRef()) {
                        circular(
                            parent,
                            (180 - buttonAngle.coerceAtMost(maxAngle)) % 360f,
                            distance
                        )
                    },
                button.interactionSource(interactionSource),
                button.shape(shape),
                button.backgroundColor(backgroundColor),
                button.contentColor(contentColor),
                button.elevation(elevation),
                button.content
            )
        }

        FloatingActionButton({ opened = !opened }, modifier.constrainAs(createRef()) {
            circular(parent, 180f, distance)
//            bottom.linkTo(parent.bottom)
//            end.linkTo(parent.end)
//            start.linkTo(parent.start)
//            top.linkTo(parent.top)
        }, interactionSource, shape, backgroundColor, contentColor, elevation) {
            AnimatedContent(targetState = opened) { isOpen ->
                if (isOpen) closeButton()
                else openButton()
            }
        }
    }
}

typealias Transformer<T> = @Composable (T) -> T

data class SpeedDialButton(
    val content: @Composable () -> Unit,
    val modifier: Transformer<Modifier> = { it },
    val interactionSource: Transformer<MutableInteractionSource> = { it },
    val shape: Transformer<Shape> = { it },
    val backgroundColor: Transformer<Color> = { it },
    val contentColor: Transformer<Color> = { it },
    val elevation: Transformer<FloatingActionButtonElevation> = { it },
    val onClick: () -> Unit,
)

public fun MutableList<SpeedDialButton>.button(
    content: @Composable () -> Unit,
    modifier: Transformer<Modifier> = { it },
    interactionSource: Transformer<MutableInteractionSource> = { it },
    shape: Transformer<Shape> = { it },
    backgroundColor: Transformer<Color> = { it },
    contentColor: Transformer<Color> = { it },
    elevation: Transformer<FloatingActionButtonElevation> = { it },
    onClick: () -> Unit,
) = add(
    SpeedDialButton(
        content,
        modifier,
        interactionSource,
        shape,
        backgroundColor,
        contentColor,
        elevation,
        onClick
    )
)

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp