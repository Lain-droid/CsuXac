package com.csuxac.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMovementListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Ignore players in creative, spectator, or flying
        if (player.getGameMode() == GameMode.CREATIVE
            || player.getGameMode() == GameMode.SPECTATOR
            || player.isFlying()
            || player.getAllowFlight()) {
            return;
        }

        // NoFall Check (Ground Spoof detection)
        // This checks if the player reports being on the ground when they are not.
        if (player.isOnGround()) {
            if (!isNearGround(player)) {
                // Flag the player and take immediate action as per the directive.
                // Kicking the player is a clear, immediate enforcement.
                final String kickMessage = "[CsuXac] Unnatural movement detected (NoFall/GroundSpoof).";

                // The kick needs to be done on the next tick to avoid issues with the event.
                player.getServer().getScheduler().runTask(player.getServer().getPluginManager().getPlugin("CsuXacCore"), () -> {
                    player.kickPlayer(kickMessage);
                });
            }
        }
    }

    private boolean isNearGround(Player player) {
        // A player's collision box is 0.6 blocks wide. We check a slightly larger area
        // just below their feet for any collidable block.
        double expand = 0.3;
        Location playerLocation = player.getLocation();

        for (double x = -expand; x <= expand; x += expand) {
            for (double z = -expand; z <= expand; z += expand) {
                // Check just below the player's location
                Block block = playerLocation.clone().add(x, -0.001, z).getBlock();

                // isCollidable() is true for solids, but also for non-full blocks like
                // slabs, stairs, fences, etc. It is false for air, grass, flowers.
                if (block.getType().isCollidable()) {
                    return true;
                }
            }
        }
        return false;
    }
}
