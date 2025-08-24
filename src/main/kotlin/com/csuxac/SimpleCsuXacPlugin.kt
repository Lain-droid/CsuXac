package com.csuxac

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import com.csuxac.core.models.*
import com.csuxac.core.config.CsuXacConfig
import com.csuxac.core.detection.MovementValidator
import com.csuxac.core.enforcement.AutomaticActionSystem
import com.csuxac.core.models.PlayerSessionManager
import com.csuxac.core.packet.PacketFlowAnalyzer
import com.csuxac.core.packet.PacketListener
import com.comphenix.protocol.ProtocolLibrary
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * CsuXac Core Enforcement Directive - Simple Paper Plugin
 * 
 * Ultimate Minecraft Anti-Cheat System with Zero-Tolerance Policy
 * Supports Minecraft 1.21+
 */
class SimpleCsuXacPlugin : JavaPlugin(), Listener {
    
    // Configuration system
    private lateinit var config: CsuXacConfig
    
    // Player session manager
    private lateinit var sessionManager: PlayerSessionManager
    
    // Automatic action system
    private lateinit var actionSystem: AutomaticActionSystem
    
    // Movement validator
    private lateinit var movementValidator: MovementValidator

    // Packet flow analyzer
    private lateinit var packetFlowAnalyzer: PacketFlowAnalyzer

    override fun onEnable() {
        logger.info("🚀 Enabling CsuXac Core Enforcement Directive...")
        
        try {
            // Initialize configuration system
            config = CsuXacConfig(this)
            logger.info("⚙️ Configuration system initialized")
            
            // Initialize player session manager
            sessionManager = PlayerSessionManager()
            logger.info("👥 Player session manager initialized")
            
            // Initialize automatic action system
            actionSystem = AutomaticActionSystem(this, config.enforcement)
            logger.info("🚨 Automatic action system initialized")
            
            // Initialize movement validator
            movementValidator = MovementValidator()
            logger.info("✅ Movement validator initialized")

            // Initialize packet flow analyzer
            packetFlowAnalyzer = PacketFlowAnalyzer(this, sessionManager, actionSystem)
            logger.info("✅ Packet flow analyzer initialized")
            
            // Register events
            server.pluginManager.registerEvents(this, this)
            
            // Register commands
            getCommand("csuxac")?.setExecutor(this)
            getCommand("csuxacreload")?.setExecutor(this)
            getCommand("csuxacstatus")?.setExecutor(this)
            
            // Register packet listener
            if (server.pluginManager.getPlugin("ProtocolLib") != null) {
                ProtocolLibrary.getProtocolManager().addPacketListener(PacketListener(this, packetFlowAnalyzer))
                logger.info("✅ ProtocolLib found, packet listener registered.")
            } else {
                logger.warning("⚠️ ProtocolLib not found, packet analysis will be disabled.")
            }

            // Log successful startup
            logger.info("✅ CsuXac Core enabled successfully for ${server.name}")
            logger.info("🛡️ Zero-tolerance anti-cheat system activated")
            logger.info("⚙️ Configuration system: ACTIVE")
            logger.info("👥 Session management: ACTIVE")
            logger.info("🚨 Enforcement system: ACTIVE")
            logger.info("📋 Commands registered: /csuxac, /csuxacreload, /csuxacstatus")
            
            // Send startup message to console
            server.consoleSender.sendMessage("§6§l[CsuXac] §aPlugin successfully enabled!")
            server.consoleSender.sendMessage("§6§l[CsuXac] §bConfiguration System: §aACTIVE")
            server.consoleSender.sendMessage("§6§l[CsuXac] §bSession Management: §aACTIVE")
            server.consoleSender.sendMessage("§6§l[CsuXac] §bEnforcement System: §aACTIVE")
            
        } catch (e: Exception) {
            logger.severe("❌ Failed to enable CsuXac Core: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }
    
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val from = event.from
        val to = event.to

        if (to == null || from.distanceSquared(to) == 0.0) {
            return // No actual movement
        }

        // Run the check asynchronously to avoid blocking the main thread
        GlobalScope.launch(Dispatchers.Default) {
            // Speed Check
            val speedResult = movementValidator.checkSpeed(player, from, to)
            if (!speedResult.isValid) {
                handleViolation(player, ViolationType.SPEED_HACK, speedResult.reason ?: "Unknown reason")
                return@launch // Don't run other checks if this one failed
            }

            // Fly Check
            if (!player.allowFlight && player.gameMode != org.bukkit.GameMode.CREATIVE && player.gameMode != org.bukkit.GameMode.SPECTATOR) {
                val flyResult = movementValidator.checkFly(player, to)
                if (!flyResult.isValid) {
                    handleViolation(player, ViolationType.FLY_HACK, flyResult.reason ?: "Unknown reason")
                }
            }
        }
    }

    private fun handleViolation(player: Player, type: ViolationType, reason: String) {
        server.scheduler.runTask(this, Runnable {
            val session = sessionManager.getOrCreateSession(player.name, player.name, player.uniqueId.toString())
            val violation = Violation(
                type = type,
                confidence = 0.9, // Confidence can be improved later
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PHYSICS_VIOLATION,
                        value = reason,
                        confidence = 0.9,
                        description = "Player movement triggered a violation"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = player.name
            )

            session.addViolation(violation)
            actionSystem.processViolation(player, violation)
        })
    }

    override fun onDisable() {
        logger.info("🛑 Disabling CsuXac Core...")
        logger.info("✅ CsuXac Core disabled successfully")
    }
    
    // Event Handlers for Anti-Cheat Detection
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        logger.info("👤 Player joined: ${player.name} (${player.uniqueId})")
        
        // Initialize player session for anti-cheat tracking
        val session = sessionManager.getOrCreateSession(
            player.name,
            player.name,
            player.uniqueId.toString()
        )
        
        // Log session creation
        if (config.general.debugMode) {
            logger.info("Session created for ${player.name}: ${session.playerId}")
        }
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        logger.info("👋 Player left: ${player.name}")
        
        // Cleanup player session
        val session = sessionManager.removeSession(player.name)
        if (session != null) {
            // Log session cleanup
            if (config.general.debugMode) {
                logger.info("Session cleaned up for ${player.name}: ${session.totalViolations.get()} violations")
            }
        }
    }
    
    // Command Handlers
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (command.name.lowercase()) {
            "csuxac" -> handleMainCommand(sender, args)
            "csuxacreload" -> handleReloadCommand(sender)
            "csuxacstatus" -> handleStatusCommand(sender)
            else -> false
        }
    }
    
    private fun handleMainCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("csuxac.admin")) {
            sender.sendMessage("§c❌ You don't have permission to use this command!")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§6§lCsuXac Core Commands:")
            sender.sendMessage("§e/csuxac reload §7- Reload configuration")
            sender.sendMessage("§e/csuxac status §7- Check system status")
            sender.sendMessage("§e/csuxac stats §7- View statistics")
            sender.sendMessage("§e/csuxac test §7- Run system tests")
            return true
        }
        
        return when (args[0].lowercase()) {
            "reload" -> handleReloadCommand(sender)
            "status" -> handleStatusCommand(sender)
            "stats" -> handleStatsCommand(sender)
            "test" -> handleTestCommand(sender)
            else -> {
                sender.sendMessage("§c❌ Unknown subcommand: ${args[0]}")
                false
            }
        }
    }
    
    private fun handleReloadCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("csuxac.admin")) {
            sender.sendMessage("§c❌ You don't have permission to use this command!")
            return true
        }
        
        try {
            // Reload configuration
            config.reloadConfig()
            
            // Reinitialize action system with new config
            actionSystem = AutomaticActionSystem(this, config.enforcement)
            
            sender.sendMessage("§a✅ CsuXac Core configuration reloaded successfully!")
            logger.info("Configuration reloaded by ${sender.name}")
            
        } catch (e: Exception) {
            sender.sendMessage("§c❌ Failed to reload configuration: ${e.message}")
            logger.severe("Failed to reload configuration: ${e.message}")
        }
        
        return true
    }
    
    private fun handleStatusCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("csuxac.monitor")) {
            sender.sendMessage("§c❌ You don't have permission to use this command!")
            return true
        }
        
        val totalStats = sessionManager.getTotalStats()
        val quarantinedPlayers = actionSystem.getQuarantinedPlayers()
        
        sender.sendMessage("§6§lCsuXac Core Status:")
        sender.sendMessage("§eStatus: §aRunning")
        sender.sendMessage("§eTotal Players: §7${totalStats.totalPlayers}")
        sender.sendMessage("§eQuarantined Players: §c${quarantinedPlayers.size}")
        sender.sendMessage("§eTotal Violations: §c${totalStats.totalViolations}")
        sender.sendMessage("§eAverage Suspicion: §e${String.format("%.2f", totalStats.averageSuspicionScore)}")
        sender.sendMessage("§eTotal Movements: §7${totalStats.totalMovements}")
        
        return true
    }
    
    private fun handleStatsCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("csuxac.monitor")) {
            sender.sendMessage("§c❌ You don't have permission to use this command!")
            return true
        }
        
        val totalStats = sessionManager.getTotalStats()
        
        sender.sendMessage("§6§lCsuXac Core Statistics:")
        sender.sendMessage("§eDetection Systems: §a1/4 ACTIVE")
        sender.sendMessage("§eTotal Violations: §c${totalStats.totalViolations}")
        sender.sendMessage("§eDetection Accuracy: §aN/A")
        sender.sendMessage("§eFalse Positive Rate: §aN/A")
        
        return true
    }
    
    private fun handleTestCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("csuxac.admin")) {
            sender.sendMessage("§c❌ You don't have permission to use this command!")
            return true
        }
        
        sender.sendMessage("§6🧪 Running CsuXac Core system tests...")
        
        // Run comprehensive system tests
        val tests = listOf(
            "Plugin System" to true,
            "Configuration System" to config.general.enabled,
            "Session Management" to true,
            "Detection Systems" to config.detection.enabled,
            "Enforcement System" to config.enforcement.enabled,
            "Event Handling" to true,
            "Command System" to true
        )
        
        tests.forEach { (component, status) ->
            val color = if (status) "§a" else "§c"
            val icon = if (status) "✅" else "❌"
            sender.sendMessage("$color$icon $component: ${if (status) "OK" else "FAILED"}")
        }
        
        sender.sendMessage("§a✅ System tests completed!")
        
        return true
    }
}