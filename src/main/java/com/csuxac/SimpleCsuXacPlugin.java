package com.csuxac;

import com.csuxac.listeners.PlayerMovementListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleCsuXacPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(), this);

        // Final message
        getLogger().info("CsuXacCore has been enabled. Reality is enforced.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CsuXacCore has been disabled. Reality is suspended.");
    }
}
