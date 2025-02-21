package de.mr_pine.taskclicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.mr_pine.taskclicker.scheduler.TaskTestScheduler
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
object Launcher

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@Composable
fun Launcher(gameManager: GameManager) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("TaskClicker", style = MaterialTheme.typography.h2, fontWeight = FontWeight.Bold)
        Text(
            "A first clicked, first served interactive gaming scheduler",
            style = MaterialTheme.typography.subtitle1,
            fontStyle = FontStyle.Italic
        )

        AutoScheduleSelector(
            gameManager.pidBlacklist,
            deselect = {
                gameManager.pidBlacklist.remove(
                    it.pid().toInt()
                )
            }) { gameManager.pidBlacklist.addAll(it.ancestors.map { it.pid().toInt() }) }

        var showTestCheckbox by remember { mutableStateOf(false) }
        var testingTimer: Duration? by remember { mutableStateOf(null) }
        val testingScope = rememberCoroutineScope()

        if (showTestCheckbox) {
            AlertDialog(
                onDismissRequest = { showTestCheckbox = false },
                confirmButton = {
                    Button(onClick = {
                        testingScope.launch(newSingleThreadContext("Test scheduler")) {
                            TaskTestScheduler.run(gameManager.pidBlacklist.toIntArray())
                            testingTimer = null
                            showTestCheckbox = false
                        }
                        testingTimer = 5.seconds
                        testingScope.launch {
                            while (testingTimer != null) {
                                delay(1.seconds)
                                testingTimer = testingTimer?.let { it - 1.seconds }
                            }
                        }
                    }) { Text("Start") }
                },
                title = { Text("Test responsiveness") },
                text = {
                    if (testingTimer == null)
                        Text("Once started, check if this application is still responsive. If not, select some more processes")
                    else
                        Text("$testingTimer")
                }
            )
        }

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.padding(top = 16.dp).width(500.dp)) {
            OutlinedButton({ showTestCheckbox = true }) {
                Text("Test")
            }

            Button({ gameManager.start() }) {
                Text("Start")
            }
        }
    }
}

@Composable
fun ColumnScope.AutoScheduleSelector(
    blackList: List<Int>,
    deselect: (ProcessHandle) -> Unit,
    select: (ProcessHandle) -> Unit
) {
    val processes = remember { ProcessHandle.allProcesses() }

    Text(
        "Select the processes necessary to run the ui so you can click your processes",
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        textAlign = TextAlign.Center
    )
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .2f))
            .weight(1f)
    ) {
        items(processes.filter { !it.guessKThread() }.toList()) { process ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(500.dp)) {
                Checkbox(
                    blackList.contains(process.pid().toInt()),
                    onCheckedChange = { if (it) select(process) else deselect(process) },
                    modifier = Modifier.height(28.dp).width(32.dp).scale(.85f)
                )
                Text(
                    "${process.name} (${process.pid()})",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}