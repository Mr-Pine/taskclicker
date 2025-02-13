package de.mr_pine.taskclicker

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import java.io.File
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

val ProcessHandle.name: String
    get() = File("/proc/${pid()}/comm").readText()

fun ProcessHandle.guessKThread() = info().command().isEmpty

class GameManager(val coroutineScope: CoroutineScope, val navigate: (Any) -> Unit) {
    val activeTasks = mutableStateListOf<Task>().apply {
        repeat(20) {
            add(
                Task(
                    "Task$it",
                    Random.Default.nextInt(10000),
                    Clock.System.now().minus(Random.Default.nextInt(500).seconds)
                )
            )
        }
    }

    fun scheduleTask(task: Task) {
        //TODO: Schedule
        activeTasks.remove(task)
    }

    fun getAncestors(): List<ProcessHandle> {
        return generateSequence(ProcessHandle.current()) { it.parent().getOrNull() }.toList()
    }

    fun getProcesses(): List<String> {
        val processes = ProcessHandle.allProcesses()
        return processes.toList().filter { !it.guessKThread() }.map { "${it.pid()}: ${it.name}" }
    }

    fun start() {
        println("Starting")
        navigate(Game)
    }
}