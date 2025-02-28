package de.mr_pine.taskclicker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.mr_pine.taskclicker.scheduler.TaskClickerScheduler
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.File
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

val ProcessHandle.name: String
    get() = File("/proc/${pid()}/comm").readText().trim()
val ProcessHandle.ancestors
    get() = generateSequence(this) { it.parent().getOrNull() }.toList()

fun ProcessHandle.guessKThread() = info().command().isEmpty

class GameManager(val coroutineScope: CoroutineScope, val navigate: (Any) -> Unit) {
    var nameBlacklist = listOf<String>()
    val pidBlacklist = mutableStateListOf<Int>().apply {
        addAll(ProcessHandle.current().ancestors.map { it.pid().toInt() })
        addAll(
            ProcessHandle.allProcesses().toList().filter { it.guessKThread() }.map(ProcessHandle::pid).map(Long::toInt)
        )
        addAll(ProcessHandle.allProcesses().toList().filter { it.name.lowercase().contains("hypr") }
            .flatMap(ProcessHandle::ancestors).map { it.pid().toInt() })
        addAll(ProcessHandle.allProcesses().toList().filter { it.name.lowercase().contains("xwayland") }
            .flatMap(ProcessHandle::ancestors).map { it.pid().toInt() })
    }

    val activeTasks = mutableStateListOf<Task>()
    var isAutoMode = false
    private var scheduler: TaskClickerScheduler? = null
    var failed by mutableStateOf(false)
    var runtime by mutableStateOf(Duration.ZERO)
    var lastPid by mutableStateOf(0)

    var syscallBalance by mutableStateOf(0)
    val powerups = listOf(Powerup(Powerup.Companion.POWERUPS.ARM, 0), Powerup(Powerup.Companion.POWERUPS.BEE, 0))

    fun scheduleTask(task: Task, isAutoOrBee: Boolean = false) {
        scheduler!!.schedule(task.tid)
        lastPid = task.tid
        activeTasks.remove(task)
        if (!isAutoOrBee) {
            scheduleRandomTasks(powerups.find { it.kind == Powerup.Companion.POWERUPS.ARM }!!.currentCount, 15)
        }
    }

    private fun scheduleRandomTasks(count: Int, from: Int = 0) {
        repeat(count) {
            if (activeTasks.size > 0 && from < activeTasks.size) {
                val task =
                    activeTasks[Random.Default.nextInt(if (activeTasks.size < from) 0 else from, activeTasks.size)]
                scheduleTask(task, true)
            }
        }
    }

    private fun ByteArray.asString() = takeWhile { it.toInt() != 0 }.map { it.toInt().toChar() }.joinToString("")

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun start() {
        navigate(Game)
        println("Starting without $pidBlacklist")
        nameBlacklist = pidBlacklist.mapNotNull {
            ProcessHandle.of(it.toLong()).getOrNull()?.name?.lowercase()
                ?.let { if (it.length < 5) it else it.substring(0, 5) }
        }
        coroutineScope.launch(newSingleThreadContext("Scheduler manager")) {
            val start = Clock.System.now()
            var beeStart = start - 2.seconds
            var beeCount = 0
            TaskClickerScheduler.run(
                {
                    val task =
                        Task(it.comm.asString(), it.pid, Instant.fromEpochMilliseconds(it.nsEntry / (1000 * 1000)))

                    val now = Clock.System.now()
                    if (now - beeStart > 1.seconds) {
                        beeStart = now
                        beeCount = powerups.find { it.kind == Powerup.Companion.POWERUPS.BEE }!!.currentCount
                    }

                    if (beeCount > 0) {
                        beeCount--
                        scheduleTask(task)
                    } else {
                        activeTasks.add(task)
                        if (isAutoMode) {
                            scheduleTask(
                                activeTasks.minBy(Task::entry), true
                            )
                        }
                    }
                }, {
                    scheduler = it
                }, {
                    if (!isAutoMode) {
                        syscallBalance += it
                    }
                }, pidBlacklist.toIntArray()
            )
            failed = true
            runtime = Clock.System.now() - start
        }
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                if (scheduler?.isRunning == true) {
                    val kthreads =
                        ProcessHandle.allProcesses().filter(ProcessHandle::guessKThread).map(ProcessHandle::pid)
                            .map(Long::toInt).toList()
                    val named = ProcessHandle.allProcesses().filter {
                        nameBlacklist.contains(it.name.let { if (it.length < 5) it else it.substring(0, 5) }
                            .lowercase())
                    }.map { it.pid().toInt() }.toList()
                    scheduler!!.updateBlacklist((kthreads + named).toIntArray())
                }
                delay(5.seconds)
            }
        }
    }

    fun stop() {
        scheduler?.close()
    }
}