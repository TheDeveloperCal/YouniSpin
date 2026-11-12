package com.youni.younispin.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.youni.younispin.visualizer.GifFit
import com.youni.younispin.visualizer.ImportedGif
import com.youni.younispin.visualizer.VisualizerSettings
import java.io.File
import kotlin.math.max

@Composable
fun GifBackgroundLayer(
    gif: ImportedGif?,
    settings: VisualizerSettings,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (gif == null || !settings.gifEnabled) return
    val context = LocalContext.current
    val file = gif.file(context)
    if (!file.exists()) return

    val animate = settings.gifAnimate &&
        !(settings.pauseGifWhenPaused && !isPlaying) &&
        !(settings.animateOnlyDuringPlayback && !isPlaying) &&
        !settings.reducedMotion
    val imageLoader = remember(context) { ImageLoader(context) }
    val imageView = remember(context) {
        ImageView(context).apply {
            scaleType = when (settings.gifFit) {
                GifFit.FILL -> ImageView.ScaleType.FIT_XY
                GifFit.FIT -> ImageView.ScaleType.FIT_CENTER
                GifFit.CENTER_CROP -> ImageView.ScaleType.CENTER_CROP
            }
            adjustViewBounds = false
        }
    }

    LaunchedEffect(file, settings.gifFps, animate, settings.gifFit) {
        (imageView.drawable as? FrameRateLimitedDrawable)?.release()
        imageView.setImageDrawable(null)
        val request = ImageRequest.Builder(context)
            .data(file)
            .size(640, 800)
            .decoderFactory(
                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoderDecoder.Factory()
                } else {
                    GifDecoder.Factory()
                }
            )
            .build()
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val drawable = FrameRateLimitedDrawable(result.drawable.mutate(), settings.gifFps)
            imageView.scaleType = when (settings.gifFit) {
                GifFit.FILL -> ImageView.ScaleType.FIT_XY
                GifFit.FIT -> ImageView.ScaleType.FIT_CENTER
                GifFit.CENTER_CROP -> ImageView.ScaleType.CENTER_CROP
            }
            imageView.setImageDrawable(drawable)
            if (animate) drawable.start() else drawable.stop()
        }
    }

    DisposableEffect(imageView, imageLoader) {
        onDispose {
            (imageView.drawable as? FrameRateLimitedDrawable)?.release()
            imageView.setImageDrawable(null)
            imageLoader.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val blurMod = if (settings.gifBlur > 0.4f && Build.VERSION.SDK_INT >= 31) {
            Modifier.blur((settings.gifBlur * 8f).dp)
        } else {
            Modifier
        }
        AndroidView(
            factory = { imageView },
            modifier = Modifier
                .fillMaxSize()
                .then(blurMod),
            update = { view ->
                view.alpha = settings.gifOpacity.coerceIn(0.05f, 1f)
                view.colorFilter = android.graphics.ColorMatrixColorFilter(
                    ColorMatrix().apply {
                        setSaturation(settings.gifSaturation.coerceIn(0f, 2f))
                        val brightness = settings.gifBrightness.coerceIn(-0.5f, 0.5f) * 255f
                        val values = this.array
                        values[4] = brightness
                        values[9] = brightness
                        values[14] = brightness
                    }
                )
                val drawable = view.drawable as? FrameRateLimitedDrawable
                if (animate) drawable?.start() else drawable?.stop()
            }
        )
        if (settings.gifDarkOverlay > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.gifDarkOverlay.coerceIn(0f, 0.85f)))
            )
        }
    }
}

private class FrameRateLimitedDrawable(
    private val source: Drawable,
    framesPerSecond: Int
) : Drawable(), Drawable.Callback, Animatable {
    private val minFrameIntervalMs = 1000L / framesPerSecond.coerceIn(15, 30)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private var cachedFrame: Bitmap? = null
    private var cachedCanvas: AndroidCanvas? = null
    private var lastDrawTimeMs = Long.MIN_VALUE
    private var dirty = true
    private val invalidateRunnable = Runnable { invalidateSelf() }

    init {
        source.callback = this
    }

    override fun draw(canvas: AndroidCanvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        val now = SystemClock.uptimeMillis()
        val current = cachedFrame
        val due = current == null || dirty || now - lastDrawTimeMs >= minFrameIntervalMs
        if (due) {
            val width = bounds.width().coerceAtMost(MAX_FRAME_DIMENSION)
            val height = bounds.height().coerceAtMost(MAX_FRAME_DIMENSION)
            if (current == null || current.width != width || current.height != height) {
                cachedFrame?.recycle()
                cachedFrame = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                cachedCanvas = AndroidCanvas(cachedFrame!!)
            }
            val bitmap = cachedFrame ?: return
            val frameCanvas = cachedCanvas ?: return
            bitmap.eraseColor(android.graphics.Color.BLACK)
            source.bounds = Rect(0, 0, width, height)
            source.draw(frameCanvas)
            lastDrawTimeMs = now
            dirty = false
        }
        cachedFrame?.let { canvas.drawBitmap(it, null, bounds, paint) }
    }

    override fun onBoundsChange(bounds: Rect) {
        dirty = true
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        source.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        source.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = source.opacity

    override fun getIntrinsicWidth(): Int = source.intrinsicWidth.coerceAtMost(MAX_FRAME_DIMENSION)
    override fun getIntrinsicHeight(): Int = source.intrinsicHeight.coerceAtMost(MAX_FRAME_DIMENSION)

    override fun start() {
        (source as? Animatable)?.start()
        dirty = true
        invalidateSelf()
    }

    override fun stop() {
        (source as? Animatable)?.stop()
    }

    override fun isRunning(): Boolean = (source as? Animatable)?.isRunning == true

    override fun invalidateDrawable(who: Drawable) {
        dirty = true
        val now = SystemClock.uptimeMillis()
        val earliest = if (lastDrawTimeMs == Long.MIN_VALUE) now else lastDrawTimeMs + minFrameIntervalMs
        unscheduleSelf(invalidateRunnable)
        scheduleSelf(invalidateRunnable, max(now, earliest))
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        scheduleSelf(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        unscheduleSelf(what)
    }

    fun release() {
        stop()
        source.callback = null
        unscheduleSelf(invalidateRunnable)
        cachedFrame?.recycle()
        cachedFrame = null
        cachedCanvas = null
    }

    companion object {
        private const val MAX_FRAME_DIMENSION = 800
    }
}

fun gifFileOrNull(context: android.content.Context, gif: ImportedGif?): File? {
    if (gif == null) return null
    val file = gif.file(context)
    return if (file.exists()) file else null
}
