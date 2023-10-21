package com.neotica.workmanagerdemo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PhotoCompressionViewModel: ViewModel() {
    private val _compressedImageUri = MutableStateFlow<Uri?>(null)
    val compressedImageUri: StateFlow<Uri?> = _compressedImageUri

    fun compressImage(inputUri: Uri, compressionThreshold: Long) {
        val inputData = workDataOf(
            PhotoCompressionWorker.KEY_CONTENT_URI to inputUri.toString(),
            PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to compressionThreshold
        )

       /* val worker = PhotoCompressionWorker(applicationContext, inputData)
        worker.enqueue()

        worker.outputData.observeForever { outputData ->
            val outputUri = outputData.getString(PhotoCompressionWorker.KEY_CONTENT_PATH)
            _compressedImageUri.value = Uri.parse(outputUri)
        }*/
    }
}