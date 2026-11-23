package com.youni.younispin.visualizer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedGif(
    val id: String,
    val fileName: String,
    val displayName: String,
    val byteSize: Long,
    val width: Int,
    val height: Int,
    val favorite: Boolean,
    val warning: String? = null
) {
    fun file(context: Context): File = File(GifBackgroundStore.dir(context), fileName)
}

class GifBackgroundStore(private val context: Context) {
    private val indexFile get() = File(dir(context), "index.json")
    private val _items = MutableStateFlow(loadIndex())
    val items: StateFlow<List<ImportedGif>> = _items.asStateFlow()

    /** Copies the bundled starter backgrounds once without disturbing user imports. */
    suspend fun seedBundledDefaults() = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences("younispin_gif_defaults", Context.MODE_PRIVATE)
        if (preferences.getBoolean("seeded", false)) return@withContext
        val defaults = listOf(
            "simp-simpsons-wave.gif" to "Simpsons Wave",
            "monkey-music.gif" to "Monkey Music",
            "vaporwave-sunset.gif" to "Vaporwave Sunset"
        )
        val next = _items.value.toMutableList()
        defaults.forEach { (assetName, displayName) ->
            val id = UUID.nameUUIDFromBytes(assetName.toByteArray(Charsets.UTF_8)).toString()
            if (next.none { it.id == id }) {
                val destination = File(dir(context), "$id.gif")
                context.assets.open("default_gifs/$assetName").use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(destination.absolutePath, bounds)
                next += ImportedGif(
                    id = id,
                    fileName = destination.name,
                    displayName = displayName,
                    byteSize = destination.length(),
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    favorite = false
                )
            }
        }
        persist(next)
        _items.value = next
        preferences.edit().putBoolean("seeded", true).apply()
    }

    suspend fun importFromUri(uri: Uri, displayNameHint: String? = null): Result<ImportedGif> =
        withContext(Dispatchers.IO) {
            try {
                val cr = context.contentResolver
                val mime = cr.getType(uri).orEmpty()
                if (mime.isNotBlank() && mime != "image/gif" && !mime.contains("gif")) {
                    return@withContext Result.failure(IllegalArgumentException("Please choose a GIF image."))
                }
                val id = UUID.randomUUID().toString()
                val temporaryFile = File(dir(context), "$id.tmp")
                val fileName = "$id.gif"
                val finalFile = File(dir(context), fileName)
                try {
                    val input = cr.openInputStream(uri)
                        ?: throw IllegalArgumentException("Could not open the selected GIF.")
                    var total = 0L
                    input.use { source ->
                        FileOutputStream(temporaryFile).use { destination ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = source.read(buffer)
                                if (read < 0) break
                                total += read
                                if (total > MAX_BYTES) {
                                    throw IllegalArgumentException("GIF is too large. Choose a file under 12 MB.")
                                }
                                destination.write(buffer, 0, read)
                            }
                        }
                    }
                    val header = ByteArray(6)
                    val validHeader = temporaryFile.inputStream().use { it.read(header) == header.size } &&
                        isGifHeader(header)
                    if (total < 16 || !validHeader) {
                        throw IllegalArgumentException("That file is not a valid GIF.")
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(temporaryFile.absolutePath, bounds)
                    val w = bounds.outWidth
                    val h = bounds.outHeight
                    if (w <= 0 || h <= 0) throw IllegalArgumentException("That file is not a valid GIF.")
                    val warning = if (w > 800 || h > 800 || total > 4 * 1024 * 1024) {
                        "Large animation — it will be downsampled to protect memory on this device."
                    } else null
                    if (!temporaryFile.renameTo(finalFile)) {
                        throw IllegalStateException("Could not save the selected GIF.")
                    }
                    val item = ImportedGif(
                        id = id,
                        fileName = fileName,
                        displayName = displayNameHint?.ifBlank { null } ?: "GIF ${items.value.size + 1}",
                        byteSize = total,
                        width = w,
                        height = h,
                        favorite = false,
                        warning = warning
                    )
                    val next = _items.value + item
                    persist(next)
                    _items.value = next
                    Result.success(item)
                } finally {
                    temporaryFile.delete()
                }
            } catch (e: OutOfMemoryError) {
                Result.failure(IllegalStateException("Not enough memory to import this GIF."))
            } catch (e: Exception) {
                Log.e(TAG, "GIF import failed", e)
                Result.failure(e)
            }
        }

    fun toggleFavorite(id: String) {
        val next = _items.value.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }
        persist(next)
        _items.value = next
    }

    fun remove(id: String) {
        val item = _items.value.find { it.id == id }
        item?.file(context)?.delete()
        val next = _items.value.filterNot { it.id == id }
        persist(next)
        _items.value = next
    }

    fun find(id: String?): ImportedGif? = id?.let { key -> _items.value.find { it.id == key } }

    private fun loadIndex(): List<ImportedGif> {
        dir(context)
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val item = ImportedGif(
                        id = o.getString("id"),
                        fileName = o.getString("fileName"),
                        displayName = o.optString("displayName", "GIF"),
                        byteSize = o.optLong("byteSize"),
                        width = o.optInt("width"),
                        height = o.optInt("height"),
                        favorite = o.optBoolean("favorite"),
                        warning = o.optString("warning").ifBlank { null }
                    )
                    if (item.file(context).exists()) add(item)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read GIF index", e)
            emptyList()
        }
    }

    private fun persist(items: List<ImportedGif>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("fileName", item.fileName)
                    .put("displayName", item.displayName)
                    .put("byteSize", item.byteSize)
                    .put("width", item.width)
                    .put("height", item.height)
                    .put("favorite", item.favorite)
                    .put("warning", item.warning ?: "")
            )
        }
        indexFile.writeText(arr.toString())
    }

    companion object {
        private const val TAG = "GifBackgroundStore"
        private const val MAX_BYTES = 12 * 1024 * 1024
        fun dir(context: Context): File =
            File(context.filesDir, "gif_backgrounds").apply { mkdirs() }

        fun isGifHeader(bytes: ByteArray): Boolean {
            if (bytes.size < 6) return false
            val header = bytes.copyOfRange(0, 6).toString(Charsets.US_ASCII)
            return header == "GIF87a" || header == "GIF89a"
        }
    }
}
