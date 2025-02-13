package de.mr_pine.taskclicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.mr_pine.taskclicker.generated.resources.Res
import de.mr_pine.taskclicker.generated.resources.ebpf_icon
import de.mr_pine.taskclicker.generated.resources.scx_logo
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource

@Serializable
object Game

const val ROWS = 3
const val COLUMNS = 4

@Composable
fun Game(gameManager: GameManager, navigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("TaskClicker", style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(navigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(painterResource(Res.drawable.ebpf_icon), "ebee", modifier = Modifier.heightIn(max = 32.dp))
            Image(painterResource(Res.drawable.scx_logo), "six armed octopus", modifier = Modifier.heightIn(max = 32.dp))
            TaskArea(gameManager.activeTasks, gameManager::scheduleTask)
        }
    }
}

@Composable
fun TaskArea(tasks: List<Task>, taskClicked: (Task) -> Unit) {
    val shuffledRelevantTasks = tasks.sortedBy(Task::entry).take(ROWS * COLUMNS).shuffled()
    Column(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
    ) {
        for (row in shuffledRelevantTasks.chunked(COLUMNS)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (task in row) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { taskClicked(task) }.fillMaxWidth().weight(1f)
                            .heightIn(min = 64.dp).clip(RoundedCornerShape(6.dp)).background(task.color)
                            .padding(8.dp)
                    ) {
                        Text(task.name, textAlign = TextAlign.Center)
                        Text(task.tid.toString(), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}