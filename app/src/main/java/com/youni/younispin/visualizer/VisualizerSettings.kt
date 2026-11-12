package com.youni.younispin.visualizer

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class VisualizerStyle(val title: String, val subtitle: String) {
    CLASSIC_EQ("Classic Equalizer", "Vertical frequency bars"),
    OSCILLOSCOPE("Oscilloscope", "Raw waveform trace"),
    SMOOTH_WAVE("Smooth Wave", "Interpolated waveform"),
    CIRCULAR_SPECTRUM("Circular Spectrum", "Radial frequency ring"),
    RADIAL_EQ("Radial Equalizer", "Spokes from center"),
    MIRRORED_BARS("Mirrored Bars", "Reflected spectrum"),
    STEREO_WAVE("Stereo Waveform", "Split / mirrored wave"),
    PARTICLE_FLOW("Particle Flow", "Audio-driven particles"),
    AUDIO_RINGS("Audio Rings", "Concentric energy rings"),
    MINIMAL_LINES("Minimal Frequency Lines", "Thin spectrum strokes"),
    ARTWORK_REACTIVE("Artwork Reactive", "Cover pulse + spectrum"),
    AMBIENT("Ambient Glow", "Soft field visualization")
}

enum class GifFit(val title: String) {
    FILL("Fill Screen"),
    FIT("Fit"),
    CENTER_CROP("Center Crop")
}

data class VisualizerSettings(
    val style: VisualizerStyle = VisualizerStyle.CLASSIC_EQ,
    val selectedGifId: String? = null,
    val gifEnabled: Boolean = true,
    val waveformEnabled: Boolean = true,
    val spectrumEnabled: Boolean = true,
    val trackInfoEnabled: Boolean = true,
    val artworkOverlayEnabled: Boolean = false,
    val decorationsEnabled: Boolean = true,
    val waveformColorHex: String = "#00D2FF",
    val accentColorHex: String = "#FFAB00",
    val lineThickness: Float = 2.2f,
    val amplitude: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val backgroundTransparency: Float = 0.35f,
    val glowIntensity: Float = 0.45f,
    val stereo: Boolean = false,
    val barCount: Int = 24,
    val barWidth: Float = 0.7f,
    val barSpacing: Float = 0.3f,
    val peakHoldMs: Int = 800,
    val sensitivity: Float = 1.0f,
    val smoothing: Float = 0.55f,
    val gifOpacity: Float = 0.85f,
    val gifBrightness: Float = 0.0f,
    val gifBlur: Float = 0f,
    val gifDarkOverlay: Float = 0.35f,
    val gifSaturation: Float = 1.0f,
    val gifSpeed: Float = 1.0f,
    val gifFps: Int = 24,
    val gifAnimate: Boolean = true,
    val gifFit: GifFit = GifFit.CENTER_CROP,
    val pauseGifWhenPaused: Boolean = true,
    val animateOnlyDuringPlayback: Boolean = true,
    val captureRateHz: Int = 20,
    val reducedMotion: Boolean = false,
    val batterySaver: Boolean = false,
    val autoReturnNowPlaying: Boolean = false
) {
    val waveformColor: Color
        get() = parseHex(waveformColorHex, Color(0xFF00D2FF))
    val accentColor: Color
        get() = parseHex(accentColorHex, Color(0xFFFFAB00))

    companion object {
        fun parseHex(hex: String, fallback: Color): Color = try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }
}

class VisualizerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("younispin_visualizer", Context.MODE_PRIVATE)

    fun load(): VisualizerSettings {
        val styleName = prefs.getString("style", VisualizerStyle.CLASSIC_EQ.name)
        val style = VisualizerStyle.entries.find { it.name == styleName } ?: VisualizerStyle.CLASSIC_EQ
        val fitName = prefs.getString("gif_fit", GifFit.CENTER_CROP.name)
        val fit = GifFit.entries.find { it.name == fitName } ?: GifFit.CENTER_CROP
        return VisualizerSettings(
            style = style,
            selectedGifId = prefs.getString("gif_id", null),
            gifEnabled = prefs.getBoolean("gif_enabled", true),
            waveformEnabled = prefs.getBoolean("waveform_enabled", true),
            spectrumEnabled = prefs.getBoolean("spectrum_enabled", true),
            trackInfoEnabled = prefs.getBoolean("track_info", true),
            artworkOverlayEnabled = prefs.getBoolean("artwork_overlay", false),
            decorationsEnabled = prefs.getBoolean("decorations", true),
            waveformColorHex = prefs.getString("wave_color", "#00D2FF") ?: "#00D2FF",
            accentColorHex = prefs.getString("accent_color", "#FFAB00") ?: "#FFAB00",
            lineThickness = prefs.getFloat("line_thickness", 2.2f),
            amplitude = prefs.getFloat("amplitude", 1.0f),
            animationSpeed = prefs.getFloat("anim_speed", 1.0f),
            backgroundTransparency = prefs.getFloat("bg_transparency", 0.35f),
            glowIntensity = prefs.getFloat("glow", 0.45f),
            stereo = prefs.getBoolean("stereo", false),
            barCount = prefs.getInt("bar_count", 24),
            barWidth = prefs.getFloat("bar_width", 0.7f),
            barSpacing = prefs.getFloat("bar_spacing", 0.3f),
            peakHoldMs = prefs.getInt("peak_hold", 800),
            sensitivity = prefs.getFloat("sensitivity", 1.0f),
            smoothing = prefs.getFloat("smoothing", 0.55f),
            gifOpacity = prefs.getFloat("gif_opacity", 0.85f),
            gifBrightness = prefs.getFloat("gif_brightness", 0f),
            gifBlur = prefs.getFloat("gif_blur", 0f),
            gifDarkOverlay = prefs.getFloat("gif_overlay", 0.35f),
            gifSaturation = prefs.getFloat("gif_sat", 1f),
            gifSpeed = prefs.getFloat("gif_speed", 1f),
            gifFps = prefs.getInt("gif_fps", 24).let { if (it in setOf(15, 24, 30)) it else 24 },
            gifAnimate = prefs.getBoolean("gif_animate", true),
            gifFit = fit,
            pauseGifWhenPaused = prefs.getBoolean("pause_gif_when_paused", true),
            animateOnlyDuringPlayback = prefs.getBoolean("animate_only_playback", true),
            captureRateHz = prefs.getInt("capture_rate", 20),
            reducedMotion = prefs.getBoolean("reduced_motion", false),
            batterySaver = prefs.getBoolean("battery_saver", false),
            autoReturnNowPlaying = prefs.getBoolean("auto_return_np", false)
        )
    }

    fun save(settings: VisualizerSettings) {
        prefs.edit()
            .putString("style", settings.style.name)
            .putString("gif_id", settings.selectedGifId)
            .putBoolean("gif_enabled", settings.gifEnabled)
            .putBoolean("waveform_enabled", settings.waveformEnabled)
            .putBoolean("spectrum_enabled", settings.spectrumEnabled)
            .putBoolean("track_info", settings.trackInfoEnabled)
            .putBoolean("artwork_overlay", settings.artworkOverlayEnabled)
            .putBoolean("decorations", settings.decorationsEnabled)
            .putString("wave_color", settings.waveformColorHex)
            .putString("accent_color", settings.accentColorHex)
            .putFloat("line_thickness", settings.lineThickness)
            .putFloat("amplitude", settings.amplitude)
            .putFloat("anim_speed", settings.animationSpeed)
            .putFloat("bg_transparency", settings.backgroundTransparency)
            .putFloat("glow", settings.glowIntensity)
            .putBoolean("stereo", settings.stereo)
            .putInt("bar_count", settings.barCount)
            .putFloat("bar_width", settings.barWidth)
            .putFloat("bar_spacing", settings.barSpacing)
            .putInt("peak_hold", settings.peakHoldMs)
            .putFloat("sensitivity", settings.sensitivity)
            .putFloat("smoothing", settings.smoothing)
            .putFloat("gif_opacity", settings.gifOpacity)
            .putFloat("gif_brightness", settings.gifBrightness)
            .putFloat("gif_blur", settings.gifBlur)
            .putFloat("gif_overlay", settings.gifDarkOverlay)
            .putFloat("gif_sat", settings.gifSaturation)
            .putFloat("gif_speed", settings.gifSpeed)
            .putInt("gif_fps", settings.gifFps)
            .putBoolean("gif_animate", settings.gifAnimate)
            .putString("gif_fit", settings.gifFit.name)
            .putBoolean("pause_gif_when_paused", settings.pauseGifWhenPaused)
            .putBoolean("animate_only_playback", settings.animateOnlyDuringPlayback)
            .putInt("capture_rate", settings.captureRateHz)
            .putBoolean("reduced_motion", settings.reducedMotion)
            .putBoolean("battery_saver", settings.batterySaver)
            .putBoolean("auto_return_np", settings.autoReturnNowPlaying)
            .apply()
    }
}
