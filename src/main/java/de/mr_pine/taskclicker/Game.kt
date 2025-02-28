package de.mr_pine.taskclicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
                IconButton(onClick = { gameManager.stop(); navigateBack() }) {
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
                for (powerup in gameManager.powerups) {
                    UpgradeCount(
                        powerup.kind.drawable,
                        powerup.currentCount,
                        powerup.kind.powerupName(powerup.currentCount)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            UpgradeButton(
                gameManager.syscallBalance,
                gameManager.powerups,
                { gameManager.syscallBalance -= it },
                { gameManager.isAutoMode = true },
                { gameManager.isAutoMode = false }
            )
        }
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
        remember(tasks.take(ROWS * COLUMNS)) { tasks.sortedBy(Task::entry).take(ROWS * COLUMNS).shuffled() }
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
                            .heightIn(min = 120.dp).clip(RoundedCornerShape(6.dp)).background(task.color)
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

@Composable
fun UpgradeButton(
    syscallBalance: Int,
    powerups: List<Powerup>,
    useSyscalls: (Int) -> Unit,
    enterAutoMode: () -> Unit,
    exitAutoMode: () -> Unit
) {
    val upgradeAvailable = powerups.any { it.nextCost?.let { it <= syscallBalance } == true }
    var upgradeMenuOpen by remember { mutableStateOf(false) }

    if (upgradeMenuOpen) {
        AlertDialog(
            onDismissRequest = { exitAutoMode(); upgradeMenuOpen = false },
            modifier = Modifier.width(550.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Buy upgrades") },
            text = {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    for (powerup in powerups.filter { it.nextCost?.let { it <= syscallBalance } == true }) {
                        ProvideTextStyle(MaterialTheme.typography.body1.copy(color = powerup.kind.foregroundColor)) {
                            Column(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(powerup.kind.color)
                                    .clickable {
                                        useSyscalls(powerup.buyNextUpgrade())
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${powerup.nextCount}", modifier = Modifier.padding(top = 8.dp))
                                Text(powerup.kind.powerupName(powerup.nextCount!!))
                                Text("${powerup.nextCost}/${syscallBalance} Syscalls")
                                Image(
                                    painterResource(powerup.kind.drawable),
                                    "powerup",
                                    modifier = Modifier.size(120.dp)
                                )
                                Text(
                                    powerup.kind.description,
                                    textAlign = TextAlign.Center,
                                    fontStyle = FontStyle.Italic,
                                    style = MaterialTheme.typography.caption,
                                    color = powerup.kind.foregroundColor,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { upgradeMenuOpen = false; exitAutoMode() }) { Text("Close") } })
    }

    Button(onClick = { enterAutoMode(); upgradeMenuOpen = true }, enabled = upgradeAvailable) {
        Text(
            "Syscall balance: $syscallBalance. " + if (upgradeAvailable) "Upgrade(s) available" else "Next upgrade at ${
                powerups.mapNotNull { it.nextCost }.min()
            }"
        )
    }
}