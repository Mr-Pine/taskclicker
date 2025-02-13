package de.mr_pine.taskclicker

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Instant

data class Task(val name: String, val tid: Int, val entry: Instant) {
    val color: Color
        get() {
            return Color.hsv((tid % 67) / 67f * 360, .8f, .8f)
        }
}
