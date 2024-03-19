package com.example.mylooper

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mylooper.ui.theme.MyLooperTheme
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
            // Handle permissions not granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )

        setContent {
            MyLooperTheme {
                val isRecording = remember { mutableStateOf(false) }
                val isPlaying = remember { mutableStateOf(false) }
                val recordedFiles = remember { mutableStateOf(getRecordedFiles()) }
                val selectedFilePath = remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = {
                        if (!isRecording.value) {
                            isRecording.value = true
                            startRecording()
                        } else {
                            isRecording.value = false
                            stopRecording()
                            recordedFiles.value = getRecordedFiles() // Refresh the list after recording
                        }
                    }) {
                        Text(if (isRecording.value) "Stop Recording" else "Start Recording")
                    }

                    Button(onClick = {
                        if (!isPlaying.value && selectedFilePath.value.isNotEmpty()) {
                            isPlaying.value = true
                            startPlaying(selectedFilePath.value, isPlaying)
                        } else {
                            isPlaying.value = false
                            stopPlaying(isPlaying)
                        }
                    }, enabled = !isRecording.value && selectedFilePath.value.isNotEmpty()) {
                        Text(if (isPlaying.value) "Stop Playing" else "Start Playing")
                    }


                    FileList(files = recordedFiles.value) { file ->
                        selectedFilePath.value = file.absolutePath
                        if (!isPlaying.value) {
                            isPlaying.value = true
                            startPlaying(selectedFilePath.value, isPlaying)
                        } else {
                            isPlaying.value = false
                            stopPlaying(isPlaying)
                        }
                    }

                }
            }
        }
    }

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            try {
                audioFilePath = "${externalCacheDir?.absolutePath}/audiorecordtest_${System.currentTimeMillis()}.aac"
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(320000)
                setOutputFile(audioFilePath)
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun startPlaying(filePath: String, isPlaying: MutableState<Boolean>) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        mediaPlayer?.setOnCompletionListener {
            stopPlaying(isPlaying)
        }
    }

    private fun stopPlaying(isPlaying: MutableState<Boolean>) {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying.value = false // Update isPlaying state here
    }


    private fun getRecordedFiles(): List<File> {
        val directory = externalCacheDir
        return directory?.listFiles { _, name -> name.endsWith(".aac") }?.toList() ?: emptyList()
    }

    @Composable
    fun FileList(files: List<File>, onFileSelected: (File) -> Unit) {
        LazyColumn {
            items(files) { file ->
                Text(file.name, modifier = Modifier.clickable { onFileSelected(file) }.padding(8.dp))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
