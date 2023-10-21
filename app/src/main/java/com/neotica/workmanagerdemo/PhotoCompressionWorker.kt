package com.neotica.workmanagerdemo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class PhotoCompressionWorker(
    private val appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val inputUri = getInputUri()
            val bytes = readBytesFromUri(inputUri) ?: return@withContext Result.failure()
            val compressedBitmap = compressBitmap(bytes)
            val outputFile = saveCompressedImage(compressedBitmap)

            Result.success(
                workDataOf(KEY_CONTENT_PATH to outputFile.absolutePath)
            )
        }
    }

    private fun getInputUri(): Uri {
        val stringUri = params.inputData.getString(KEY_CONTENT_URI)
        return Uri.parse(stringUri)
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return appContext.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        }
    }

    private fun compressBitmap(inputBytes: ByteArray): Bitmap {
        val initialQuality = 100
        var quality = initialQuality
        val thresholdBytes = params.inputData.getLong(KEY_COMPRESSION_THRESHOLD, 0L)

        val originalBitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)

        do {
            val outputStream = ByteArrayOutputStream()
            outputStream.use {
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
                val compressedBytes = outputStream.toByteArray()
                if (compressedBytes.size <= thresholdBytes) {
                    return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                }
                quality -= (quality * 0.1).roundToInt()
            }
        } while (quality > 5)

        return originalBitmap
    }

    private fun saveCompressedImage(bitmap: Bitmap): File {
        val file = File(applicationContext.cacheDir, "${params.id}.jpg")
        file.writeBitmapToFile(bitmap)

        // Save to the library
        //saveImageToLibrary(bitmap)

        return file
    }

    private fun File.writeBitmapToFile(bitmap: Bitmap) {
        val fos = outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos)
        fos.close()
    }

    private fun saveImageToLibrary(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "image_${params.id}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NeoFolder")
        }

        val resolver = appContext.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val fos = resolver.openOutputStream(imageUri!!)!!
        fos.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 30, it) }
    }

    companion object {
        const val KEY_CONTENT_URI = "KEY_CONTENT_URI"
        const val KEY_COMPRESSION_THRESHOLD = "KEY_COMPRESSION_THRESHOLD"
        const val KEY_CONTENT_PATH = "KEY_RESULT_PATH"
    }
}