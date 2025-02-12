package de.mr_pine.taskclicker

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

@Serializable
object Game

@Composable
fun Game(gameManager: GameManager) {
    Text("game")
}