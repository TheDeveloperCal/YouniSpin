package com.youni.younispin.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

@Composable
fun Modifier.centerJoystick(
    onTap: () -> Unit,
    onMove: (direction: Int) -> Unit,
    onDirectionChanged: (direction: Int) -> Unit,
    onPressedChanged: (pressed: Boolean) -> Unit,
    deadZone: Dp = 10.dp,
    directionThreshold: Dp = 22.dp,
    initialRepeatDelayMs: Long = 360L,
    repeatIntervalMs: Long = 210L
): Modifier {
    val latestOnTap = rememberUpdatedState(onTap)
    val latestOnMove = rememberUpdatedState(onMove)
    val latestOnDirectionChanged = rememberUpdatedState(onDirectionChanged)
    val latestOnPressedChanged = rememberUpdatedState(onPressedChanged)
    return pointerInput(deadZone, directionThreshold, initialRepeatDelayMs, repeatIntervalMs) {
    val touchSlop = viewConfiguration.touchSlop
    val deadZonePx = maxOf(deadZone.toPx(), touchSlop)
    val directionThresholdPx = maxOf(directionThreshold.toPx(), deadZonePx + 4.dp.toPx())

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        val pointerId: PointerId = down.id
        val origin = down.position
        down.consume()
        latestOnPressedChanged.value(true)

        var activeDirection = 0
        var nextRepeatAt = Long.MAX_VALUE
        var maximumDisplacement = 0f
        var movedToDirection = false
        var completedNormally = false
        try {
            while (true) {
                val now = android.os.SystemClock.uptimeMillis()
                val timeout = if (activeDirection == 0) null else (nextRepeatAt - now).coerceAtLeast(1L)
                val event = if (timeout == null) {
                    awaitPointerEvent(PointerEventPass.Main)
                } else {
                    withTimeoutOrNull(timeout) { awaitPointerEvent(PointerEventPass.Main) }
                }

                if (event == null) {
                    latestOnMove.value(activeDirection)
                    nextRepeatAt = android.os.SystemClock.uptimeMillis() + repeatIntervalMs
                    continue
                }

                val change = event.changes.firstOrNull { it.id == pointerId }
                    ?: event.changes.firstOrNull()
                    ?: continue
                change.consume()
                if (!change.pressed) {
                    completedNormally = true
                    break
                }

                val delta = change.position - origin
                val displacement = delta.getDistance()
                maximumDisplacement = maxOf(maximumDisplacement, displacement)
                val absX = abs(delta.x)
                val absY = abs(delta.y)
                val nextDirection = when {
                    absY <= deadZonePx || absY <= absX -> 0
                    absY >= directionThresholdPx -> if (delta.y < 0f) -1 else 1
                    else -> 0
                }

                if (nextDirection != activeDirection) {
                    activeDirection = nextDirection
                    latestOnDirectionChanged.value(activeDirection)
                    if (activeDirection != 0) {
                        movedToDirection = true
                        latestOnMove.value(activeDirection)
                        nextRepeatAt = android.os.SystemClock.uptimeMillis() + initialRepeatDelayMs
                    } else {
                        nextRepeatAt = Long.MAX_VALUE
                    }
                }
            }
        } finally {
            if (activeDirection != 0) latestOnDirectionChanged.value(0)
            latestOnPressedChanged.value(false)
        }

        if (completedNormally && !movedToDirection && maximumDisplacement <= deadZonePx) {
            latestOnTap.value()
        }
    }
    }
}
