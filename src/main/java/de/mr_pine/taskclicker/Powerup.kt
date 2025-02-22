package de.mr_pine.taskclicker

import androidx.compose.ui.graphics.Color
import de.mr_pine.taskclicker.generated.resources.Res
import de.mr_pine.taskclicker.generated.resources.ebpf_icon
import de.mr_pine.taskclicker.generated.resources.scx_logo
import org.jetbrains.compose.resources.DrawableResource

data class Powerup(val kind: POWERUPS, var currentCount: Int) {
    private var upgrades = kind.upgrades.toList()

    val nextCost
        get() = upgrades.firstOrNull()?.cost
    val nextCount
        get() = upgrades.firstOrNull()?.count

    fun buyNextUpgrade(): Int {
        val upgrade = upgrades[0]
        upgrades = upgrades.drop(1)
        currentCount += upgrade.count
        return upgrade.cost
    }

    companion object {
        data class Upgrade(val count: Int, val cost: Int)

        enum class POWERUPS(val upgrades: List<Upgrade>, val drawable: DrawableResource, val powerupName: (Int) -> String, val color: Color, val foregroundColor: Color) {
            ARM(listOf(Upgrade(1, 5000)), Res.drawable.scx_logo, { if (it == 1) "extra arm" else "extra arms" }, Color(0x0F, 0x61, 0x55), Color.White),
            BEE(listOf(Upgrade(1, 5000)), Res.drawable.ebpf_icon, { if (it == 1) "eBee" else "eBees" }, Color(0xFF, 0xE1, 0x00), Color.Black);
        }
    }
}
