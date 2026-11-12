package com.youni.younispin.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youni.younispin.ui.theme.*
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun OrbitDial(
    modifier: Modifier = Modifier,
    dialSizeDp: Int = 250,
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {},
    onCenterClick: () -> Unit = {},
    onCenterNavigateUp: () -> Unit = {},
    onCenterNavigateDown: () -> Unit = {},
    onTopClick: () -> Unit = {},
    onBottomClick: () -> Unit = {},
    onLeftClick: () -> Unit = {},
    onRightClick: () -> Unit = {},
    isPlaying: Boolean = false,
    hapticEnabled: Boolean = true,
    animationsEnabled: Boolean = true
) {
    val colors = YouniTheme.colors
    val view = LocalView.current
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var accumulatedAngle by remember { mutableFloatStateOf(0f) }
    val angleThresholdDegrees = 14f
    var centerPressed by remember { mutableStateOf(false) }
    var joystickDirection by remember { mutableIntStateOf(0) }
    val centerScale by animateFloatAsState(
        targetValue = if (centerPressed && animationsEnabled) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "orbitCenterPress"
    )
    val centerOffsetY by animateFloatAsState(
        targetValue = if (animationsEnabled) joystickDirection * 5f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "orbitCenterJoystick"
    )

    fun triggerHaptic() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun moveSelection(direction: Int) {
        if (direction == 0) return
        if (hapticEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        if (direction < 0) onCenterNavigateUp() else onCenterNavigateDown()
    }

    Box(
        modifier = modifier
            .size(dialSizeDp.dp)
            .semantics { contentDescription = "Orbit Dial navigation controller" },
        contentAlignment = Alignment.Center
    ) {
        // Outer Rotating Ring Surface with Touch Gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.dialDisc)
                .border(2.dp, colors.dialDiscBorder, CircleShape)
                .pointerInput(Unit) {
                    var lastAngleRad: Float? = null
                    var centerGesture = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            centerGesture = (offset - center).getDistance() <= minOf(size.width, size.height) * 0.24f
                            lastAngleRad = if (centerGesture) null
                            else atan2(offset.y - center.y, offset.x - center.x)
                        },
                        onDrag = { change, _ ->
                            if (!centerGesture) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val currentAngleRad = atan2(change.position.y - center.y, change.position.x - center.x)
                                val prev = lastAngleRad

                                if (prev != null) {
                                    var deltaRad = currentAngleRad - prev
                                    if (deltaRad > PI) deltaRad -= (2 * PI).toFloat()
                                    if (deltaRad < -PI) deltaRad += (2 * PI).toFloat()

                                    val deltaDeg = (deltaRad * 180f / PI).toFloat()
                                    rotationAngle = (rotationAngle + deltaDeg) % 360f
                                    accumulatedAngle += deltaDeg

                                    if (accumulatedAngle >= angleThresholdDegrees) {
                                        onRotateClockwise()
                                        triggerHaptic()
                                        accumulatedAngle = 0f
                                    } else if (accumulatedAngle <= -angleThresholdDegrees) {
                                        onRotateCounterClockwise()
                                        triggerHaptic()
                                        accumulatedAngle = 0f
                                    }
                                }
                                lastAngleRad = currentAngleRad
                            }
                        },
                        onDragEnd = {
                            lastAngleRad = null
                            accumulatedAngle = 0f
                            centerGesture = false
                        },
                        onDragCancel = {
                            lastAngleRad = null
                            accumulatedAngle = 0f
                            centerGesture = false
                        }
                    )
                }
        ) {
            // Drawn Concentric Grooves and Orbit Ring Indicator
            val ringColor = colors.dialRingBorder
            val accentColor = colors.accent
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.width / 2f - 4.dp.toPx()
                val innerRadius = outerRadius * 0.44f

                // Subtle industrial grooved rings
                drawCircle(
                    color = ringColor,
                    radius = (outerRadius + innerRadius) / 2f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.6f),
                    radius = outerRadius * 0.82f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Orbit tracking indicator dot on ring
                val rad = (rotationAngle * PI / 180f).toFloat()
                val orbitDotRadius = outerRadius * 0.72f
                val dotX = center.x + orbitDotRadius * kotlin.math.cos(rad)
                val dotY = center.y + orbitDotRadius * kotlin.math.sin(rad)

                drawCircle(
                    color = accentColor,
                    radius = 4.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // Directional Buttons (Top: MENU, Bottom: PLAY/PAUSE, Left: PREV, Right: NEXT)
            // Top: MENU
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(CircleShape)
                    .clickable {
                        triggerHaptic()
                        onTopClick()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Orbit Dial Menu" }
            ) {
                Text(
                    text = "MENU",
                    color = colors.dialLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Bottom: PLAY / PAUSE
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(CircleShape)
                    .clickable {
                        triggerHaptic()
                        onBottomClick()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) colors.dialLabelActive else colors.dialLabel,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Left: PREV / REWIND
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .clip(CircleShape)
                    .clickable {
                        triggerHaptic()
                        onLeftClick()
                    }
                    .padding(8.dp)
                    .semantics { contentDescription = "Previous Track" }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = colors.dialLabel,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right: NEXT / FORWARD
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .clip(CircleShape)
                    .clickable {
                        triggerHaptic()
                        onRightClick()
                    }
                    .padding(8.dp)
                    .semantics { contentDescription = "Next Track" }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = colors.dialLabel,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Central Tactile Select Hub Button
        val centerSize = (dialSizeDp * 0.42f).dp
        Box(
            modifier = Modifier
                .size(centerSize)
                .scale(centerScale)
                .offset(y = centerOffsetY.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.dialHub)
                .border(1.5.dp, colors.dialHubBorder, CircleShape)
                .centerJoystick(
                    onTap = {
                        if (hapticEnabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onCenterClick()
                    },
                    onMove = ::moveSelection,
                    onDirectionChanged = { joystickDirection = it },
                    onPressedChanged = { centerPressed = it }
                )
                .semantics {
                    contentDescription = "Select. Drag up for previous item or down for next item."
                    onClick(label = "Select") {
                        onCenterClick()
                        true
                    }
                    customActions = listOf(
                        CustomAccessibilityAction("Previous item") {
                            onCenterNavigateUp()
                            true
                        },
                        CustomAccessibilityAction("Next item") {
                            onCenterNavigateDown()
                            true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (joystickDirection != 0) {
                Icon(
                    imageVector = if (joystickDirection < 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) colors.secondaryAccent else colors.dialRingBorder)
                )
            }
        }
    }
}
