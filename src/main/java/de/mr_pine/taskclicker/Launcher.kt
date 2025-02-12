package de.mr_pine.taskclicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
object Launcher

@Composable
fun Launcher(gameManager: GameManager) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("TaskClicker", style = MaterialTheme.typography.h2, fontWeight = FontWeight.Bold)
        Text("A first-click-first-served interactive gaming scheduler", style = MaterialTheme.typography.subtitle1, fontStyle = FontStyle.Italic)
        Button({gameManager.start()}, modifier = Modifier.padding(top = 16.dp)) {
            Text("Start")
        }
    }
}