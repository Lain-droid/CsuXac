package com.csuxac.plugin

import com.csuxac.common.EventBus
import com.csuxac.common.events.MovementEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin

class CsuXacPlugin : JavaPlugin(), Listener {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val loc = player.location
        val movementEvent = MovementEvent(
            playerId = player.uniqueId.toString(),
            timestamp = System.currentTimeMillis(),
            x = loc.x,
            y = loc.y,
            z = loc.z,
            onGround = player.isOnGround
        )
        scope.launch {
            EventBus.publish(movementEvent)
        }
    }
}