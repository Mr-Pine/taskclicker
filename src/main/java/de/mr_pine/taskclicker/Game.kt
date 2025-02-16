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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.mr_pine.taskclicker.generated.resources.Res
import de.mr_pine.taskclicker.generated.resources.ebpf_icon
import de.mr_pine.taskclicker.generated.resources.scx_logo
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

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
        if (gameManager.failed) {
            Text("Failed after ${gameManager.runtime}", color = Color.Red)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TaskArea(gameManager.activeTasks, gameManager.lastPid, gameManager::scheduleTask)
            Column(modifier = Modifier) {
                UpgradeCount(
                    Res.drawable.scx_logo,
                    gameManager.extraArmCount,
                    if (gameManager.ebeeCount == 1) "extra arm" else "extra arms"
                )
                UpgradeCount(
                    Res.drawable.ebpf_icon,
                    gameManager.ebeeCount,
                    if (gameManager.ebeeCount == 1) "eBee" else "eBees"
                )
            }
        }
        Text("Current syscall balance: ${gameManager.syscallBalance}")
    }
}

@Composable
fun UpgradeCount(icon: DrawableResource, count: Int, name: String) {
    Row(modifier = Modifier.padding(4.dp)) {
        Box(modifier = Modifier.padding(end = 8.dp)) {
            if (count > 0) {
                Image(
                    painterResource(icon),
                    contentDescription = name,
                    alpha = 0.5f,
                    modifier = Modifier.size(24.dp).offset(4.dp, 4.dp)
                )
            }
            Image(painterResource(icon), contentDescription = name, Modifier.size(24.dp))
        }
        Text("$count $name")
    }
}

@Composable
fun RowScope.TaskArea(tasks: List<Task>, lastPid: Int, taskClicked: (Task) -> Unit) {
    val now = remember(lastPid, tasks.size >= ROWS * COLUMNS) { Clock.System.now() }
    val shuffledRelevantTasks =
        remember(lastPid, tasks.size >= ROWS * COLUMNS) { tasks.sortedBy(Task::entry).take(ROWS * COLUMNS).shuffled() }
    Column(
        modifier = Modifier.weight(1f).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
    ) {
        for (row in shuffledRelevantTasks.chunked(COLUMNS)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (task in row) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { taskClicked(task) }.fillMaxWidth().weight(1f)
                            .heightIn(min = 96.dp).clip(RoundedCornerShape(6.dp)).background(task.color)
                            .padding(8.dp)
                    ) {
                        Text(task.name, textAlign = TextAlign.Center)
                        Text(task.tid.toString(), textAlign = TextAlign.Center)
                        Text("${(task.entry - now).inWholeMilliseconds.milliseconds}")
                    }
                }
            }
        }
    }
}