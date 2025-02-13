package de.mr_pine.taskclicker

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "TaskClicker") {
        MaterialTheme {
            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()
            val gameManager = remember { GameManager(coroutineScope, navController::navigate) }

            NavHost(navController, startDestination = Launcher) {
                composable<Launcher> {
                    Launcher(gameManager)
                }
                composable<Game> {
                    Game(gameManager, navigateBack = { navController.navigate(Launcher) })
                }
            }
        }
    }
}
