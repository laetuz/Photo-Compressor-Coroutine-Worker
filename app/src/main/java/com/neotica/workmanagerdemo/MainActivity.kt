package com.neotica.workmanagerdemo

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.neotica.workmanagerdemo.ui.theme.WorkManagerDemoTheme

class MainActivity : ComponentActivity() {

    //Step 13.1 lateinit the workmanager
    private lateinit var workManager: WorkManager
    //Step 15 instantiate viewmodel
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Step 13.2
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
                    viewModel.compressedBitmap?.let {
                        val compressedText = "Compressed photo"
                        Text(text = "$compressedText:")
                        Image(bitmap = it.asImageBitmap(), contentDescription = compressedText)
                       // AsyncImage(model = it, contentDescription = compressedText)
                    }
                    Text(text = "Howdy?")
                }
            }
        }
    }

    //Step 12 run the tasks on newIntent
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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
        //Step 13.3
        workManager.enqueue(request)
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