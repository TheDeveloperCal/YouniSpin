package com.youni.younispin.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

object ArtworkManager {
    private const val TAG = "ArtworkManager"

    private fun getArtworkDir(context: Context): File {
        val dir = File(context.filesDir, "artwork")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveArtworkFromUri(context: Context, uri: Uri, prefix: String = "custom"): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return null

            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val file = File(getArtworkDir(context), fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save artwork from uri", e)
            null
        }
    }

    fun saveArtworkBytes(context: Context, bytes: ByteArray, prefix: String = "art"): String? {
        return try {
            val md5 = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }
            val file = File(getArtworkDir(context), "${prefix}_$md5.jpg")
            if (file.exists()) {
                return file.absolutePath
            }
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save artwork bytes", e)
            null
        }
    }

    fun deleteArtwork(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists() && file.parentFile?.name == "artwork") {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete artwork file", e)
        }
    }
}
