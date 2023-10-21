package com.neotica.workmanagerdemo

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.neotica.workmanagerdemo.ui.theme.WorkManagerDemoTheme
import java.io.File
import java.io.OutputStream
import java.util.Objects

class MainActivity : ComponentActivity() {

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayListOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    //Step 13.1 lateinit the workmanager
    private lateinit var workManager: WorkManager
    //Step 15 instantiate viewmodel
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Step 13.2
        if (checkMultiplePermission()) {
            doOperation()
        }
        workManager = WorkManager.getInstance(applicationContext)
        setContent {
            WorkManagerDemoTheme {
                // A surface container using the 'background' color from the theme
                //Step 17
                val workerResult = viewModel.workId?.let {
                    workManager.getWorkInfoByIdLiveData(it).observeAsState().value
                }
                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        val filePath =
                            workerResult.outputData.getString(PhotoCompressionWorker.KEY_CONTENT_PATH)
                        filePath?.let {
                            val bitmap = BitmapFactory.decodeFile(it)
                            viewModel.updateCompressedBitmap(bitmap)
                        }
                    }
                }

                LazyColumn {
                    item {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            viewModel.uncompressedUri?.let {
                                val uncompressedText = "Uncompressed photo"
                                Text(text = "$uncompressedText:")
                                AsyncImage(model = it, contentDescription = uncompressedText)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            if (viewModel.compressedBitmap == null) {
                                Text(text = "Howdy?")
                                CircularProgressIndicator()
                            } else {
                                viewModel.compressedBitmap?.let {
                                    val compressedText = "Compressed photo"
                                    Text(text = "$compressedText:")
                                    Image(bitmap = it.asImageBitmap(), contentDescription = compressedText)
                                    Button(onClick = {
                                        saveImageToLibrary(it)
                                    }) {
                                        Text("Download")
                                    }
                                }
                            }


                        }
                    }
                }

            }
        }
    }

    //Step 12.1 run the tasks on newIntent
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //Step 12.2
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        //Step 16.1
        viewModel.updateUncompressUri(uri)
        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData(
                workDataOf(
                    PhotoCompressionWorker.KEY_CONTENT_URI to uri.toString(),
                    PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            //.setConstraints(Constraints(requiresStorageNotLow = true))
            .build()
        //Step 16.2
        viewModel.updateWorkId(request.id)
        //Step 16.3
        workManager.enqueue(request)
    }

    //Save image
    private fun saveImageToLibrary(bitmap: Bitmap){
        val resolver = contentResolver
        val contentValues = ContentValues()
        val fos: OutputStream
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image_" + ".jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+File.separator+"NeoFolder")
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        fos = resolver.openOutputStream(Objects.requireNonNull(imageUri)!!)!!
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        Objects.requireNonNull(fos)
    }

    //Storage management
    private fun doOperation() {
        Log.d("neo-storage", "All permission granted")
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    // here all permission granted successfully
                    doOperation()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        // here app Setting open because all permission is not granted
                        // and permanent denied
                       // appSettingOpen(this)
                    } else {
                        // here warning permission show
                        /*warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }*/
                    }
                }
            }
        }
    }

    //Save file
    private fun saveFileToGallery() {
        // >> Android Q
        val fos: OutputStream
        val resolver = contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "image_${dataDir.absoluteFile}" + "jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+File.separator+"TestFolder")
        //val imageUri = resolver.insert()

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkManagerDemoTheme {
        Greeting("Android")
    }
}