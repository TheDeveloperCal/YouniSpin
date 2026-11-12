package com.youni.younispin.playback

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(
    private val onTimerExpired: () -> Unit,
    private val onFadeVolume: (Float) -> Unit = {}
) {
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _remainingMs = MutableStateFlow<Long>(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    fun startTimer(durationMinutes: Int) {
        cancelTimer()
        if (durationMinutes <= 0) return

        val totalMs = durationMinutes * 60 * 1000L
        _remainingMs.value = totalMs
        _isTimerActive.value = true

        timerJob = scope.launch {
            var left = totalMs
            val fadeWindowMs = 15_000L
            while (left > 0 && isActive) {
                delay(1000)
                left -= 1000
                _remainingMs.value = left.coerceAtLeast(0L)
                if (left in 1 until fadeWindowMs) {
                    onFadeVolume((left.toFloat() / fadeWindowMs).coerceIn(0f, 1f))
                }
            }
            onFadeVolume(0f)
            _isTimerActive.value = false
            _remainingMs.value = 0L
            onTimerExpired()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerActive.value = false
        _remainingMs.value = 0L
        onFadeVolume(1f)
    }
}
