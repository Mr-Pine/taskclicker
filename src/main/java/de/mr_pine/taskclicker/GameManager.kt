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
import kotlin.time.Duration

val ProcessHandle.name: String
    get() = File("/proc/${pid()}/comm").readText().trim()
val ProcessHandle.ancestors
    get() = generateSequence(this) { it.parent().getOrNull() }.toList()

fun ProcessHandle.guessKThread() = info().command().isEmpty

class GameManager(val coroutineScope: CoroutineScope, val navigate: (Any) -> Unit) {
    val pidBlacklist = mutableStateListOf<Int>().apply {
        addAll(ProcessHandle.current().ancestors.map { it.pid().toInt() })
        addAll(
            ProcessHandle.allProcesses().toList().filter { it.guessKThread() }.map(ProcessHandle::pid)
                .map(Long::toInt)
        )
        addAll(ProcessHandle.allProcesses().toList().filter { it.name.lowercase().contains("hypr") }
            .flatMap(ProcessHandle::ancestors).map { it.pid().toInt() })
        addAll(ProcessHandle.allProcesses().toList().filter { it.name.lowercase().contains("xwayland") }
            .flatMap(ProcessHandle::ancestors).map { it.pid().toInt() })
    }

    val activeTasks = mutableStateListOf<Task>()/*.apply {
        repeat(20) {
            add(
                Task(
                    "Task$it",
                    Random.Default.nextInt(10000),
                    Clock.System.now().minus(Random.Default.nextInt(500).seconds)
                )
            )
        }
    }*/
    var scheduler: TaskClickerScheduler? = null
    var failed by mutableStateOf(false)
    var runtime by mutableStateOf(Duration.ZERO)
    var lastPid by mutableStateOf(0)

    var syscallBalance by mutableStateOf(0)
    var extraArmCount by mutableStateOf(0)
    var ebeeCount by mutableStateOf(5)

    fun scheduleTask(task: Task) {
        scheduler!!.schedule(task.tid)
        lastPid = task.tid
        activeTasks.remove(task)
    }

    fun getProcesses(): List<String> {
        val processes = ProcessHandle.allProcesses()
        return processes.toList().filter { !it.guessKThread() }.map { "${it.pid()}: ${it.name}" }
    }

    private fun ByteArray.asString() = takeWhile { it.toInt() != 0 }.map { it.toInt().toChar() }.joinToString("")

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun start() {
        navigate(Game)
        println("Starting without $pidBlacklist")
        coroutineScope.launch(newSingleThreadContext("Scheduler manager")) {
            val start = Clock.System.now()
            TaskClickerScheduler.run(
                {
                    activeTasks.add(
                        Task(
                            it.comm.asString(), it.pid, Instant.fromEpochMilliseconds(it.nsEntry / (1000 * 1000))
                        )
                    )
                    //println("$it: ${it.comm.asString()}")
                    if (false && activeTasks.size > 15) {
                        scheduleTask(
                            activeTasks.minBy(Task::entry)
                        )
                    }
                }, {
                    scheduler = it
                }, {
                    syscallBalance += it
                }, pidBlacklist.toIntArray()
            )
            failed = true
            runtime = Clock.System.now() - start
        }
    }
}