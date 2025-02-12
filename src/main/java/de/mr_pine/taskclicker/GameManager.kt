package de.mr_pine.taskclicker

import kotlinx.coroutines.CoroutineScope
import java.io.File
import kotlin.jvm.optionals.getOrNull

val ProcessHandle.name: String
    get() = File("/proc/${pid()}/comm").readText()

fun ProcessHandle.guessKThread() = info().command().isEmpty

class GameManager(val coroutineScope: CoroutineScope, val navigate: (Any) -> Unit) {
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