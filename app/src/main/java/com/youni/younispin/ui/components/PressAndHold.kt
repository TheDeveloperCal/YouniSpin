package com.youni.younispin.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

fun Modifier.pressAndHold(
    onTap: () -> Unit,
    onHoldStart: (() -> Unit)? = null,
    onHoldEnd: (() -> Unit)? = null,
    onPressedChange: ( (Boolean) -> Unit)? = null,
    longPressMs: Long = 400L
): Modifier = pointerInput(onTap, onHoldStart, onHoldEnd, onPressedChange, longPressMs) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onPressedChange?.invoke(true)
        var holdStarted = false
        try {
            val releasedBeforeHold = withTimeoutOrNull(longPressMs) {
                waitForUpOrCancellation()
            }
            if (releasedBeforeHold != null) {
                onTap()
            } else {
                holdStarted = true
                onHoldStart?.invoke()
                waitForUpOrCancellation()
            }
        } finally {
            if (holdStarted) onHoldEnd?.invoke()
            onPressedChange?.invoke(false)
        }
    }
}
