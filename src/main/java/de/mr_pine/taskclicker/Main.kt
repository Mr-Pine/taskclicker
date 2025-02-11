package de.mr_pine.taskclicker

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.mr_pine.taskclicker.scheduler.TaskClickerScheduler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlin.coroutines.coroutineContext

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main() = application {
    LaunchedEffect(Unit) {
        launch(newSingleThreadContext("Scheduler manager")) {
            TaskClickerScheduler.aaaa()
        }
    }
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
