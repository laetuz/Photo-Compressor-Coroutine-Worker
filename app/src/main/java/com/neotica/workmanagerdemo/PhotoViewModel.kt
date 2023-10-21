package com.neotica.workmanagerdemo

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID

//Step 14
class PhotoViewModel(application: Application): ViewModel() {
    val workInfo: LiveData<WorkInfo>?
        get() = _workInfo

    private val _workInfo: LiveData<WorkInfo>? = null



    var isWorkerRunning: Boolean by mutableStateOf(false)

    var uncompressedUri: Uri? by mutableStateOf(null)
        private set

    var compressedBitmap: Bitmap? by mutableStateOf(null)
        private set

    var workId: UUID? by mutableStateOf(null)
        private set

    fun updateUncompressUri(uri: Uri?) {
        isWorkerRunning = true
        uncompressedUri = uri
    }

    fun updateCompressedBitmap(bitmap: Bitmap?) {
        isWorkerRunning = false
        compressedBitmap = bitmap
    }

    fun updateWorkId(uuid:UUID?) {
        workId = uuid
    }
}