package com.csuxac.core.enforcement

import com.csuxac.core.models.*
import com.csuxac.core.config.EnforcementConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Automatic Action System for CsuXac Anti-Cheat
 * Handles automatic responses to violations
 */
class AutomaticActionSystem(
    private val plugin: Plugin,
    private val config: EnforcementConfig
) {
    
    private val playerViolationCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val playerActionHistory = ConcurrentHashMap<String, MutableList<EnforcementAction>>()
    private val quarantinedPlayers = ConcurrentHashMap<String, QuarantineInfo>()
    
    /**
     * Process violation and take appropriate action
     */
    fun processViolation(player: Player, violation: Violation): EnforcementResult {
        val playerId = player.uniqueId.toString()
        val violationCount = playerViolationCounts.getOrPut(playerId) { AtomicInteger(0) }
        val currentCount = violationCount.incrementAndGet()
        
        // Log violation
        logViolation(player, violation, currentCount)
        
        // Determine action based on violation severity and count
        val action = determineAction(violation.severity, currentCount)
        
        // Execute action
        val result = executeAction(player, action, violation)
        
        // Update action history
        updateActionHistory(playerId, action, violation)
        
        // Check for quarantine
        if (config.quarantineEnabled && currentCount >= config.quarantineThreshold) {
            quarantinePlayer(player, "Multiple violations detected")
        }
        
        return result
    }
    
    /**
     * Determine appropriate action based on violation severity and count
     */
    private fun determineAction(severity: ViolationSeverity, count: Int): EnforcementAction {
        return when {
            severity == ViolationSeverity.CRITICAL -> EnforcementAction.BAN
            severity == ViolationSeverity.HIGH && count >= 3 -> EnforcementAction.KICK
            severity == ViolationSeverity.HIGH -> EnforcementAction.WARNING
            severity == ViolationSeverity.MEDIUM && count >= 5 -> EnforcementAction.KICK
            severity == ViolationSeverity.MEDIUM -> EnforcementAction.WARNING
            severity == ViolationSeverity.LOW && count >= 10 -> EnforcementAction.WARNING
            else -> EnforcementAction.NOTIFY
        }
    }
    
    /**
     * Execute the determined action
     */
    private fun executeAction(player: Player, action: EnforcementAction, violation: Violation): EnforcementResult {
        return when (action) {
            EnforcementAction.BAN -> executeBan(player, violation)
            EnforcementAction.KICK -> executeKick(player, violation)
            EnforcementAction.WARNING -> executeWarning(player, violation)
            EnforcementAction.QUARANTINE -> executeQuarantine(player, violation)
            EnforcementAction.NOTIFY -> executeNotify(player, violation)
        }
    }
    
    /**
     * Execute ban action
     */
    private fun executeBan(player: Player, violation: Violation): EnforcementResult {
        if (!config.autoBan) {
            return EnforcementResult(
                action = EnforcementAction.BAN,
                success = false,
                reason = "Auto-ban is disabled in configuration",
                timestamp = System.currentTimeMillis()
            )
        }
        
        try {
            // Ban the player
            val banReason = "CsuXac Anti-Cheat: ${violation.type} (Severity: ${violation.severity})"
            player.banPlayer(banReason)
            
            // Notify admins
            notifyAdmins(player, "BANNED", violation)
            
            return EnforcementResult(
                action = EnforcementAction.BAN,
                success = true,
                reason = banReason,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to ban player ${player.name}: ${e.message}")
            return EnforcementResult(
                action = EnforcementAction.BAN,
                success = false,
                reason = "Error: ${e.message}",
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Execute kick action
     */
    private fun executeKick(player: Player, violation: Violation): EnforcementResult {
        if (!config.autoKick) {
            return EnforcementResult(
                action = EnforcementAction.KICK,
                success = false,
                reason = "Auto-kick is disabled in configuration",
                timestamp = System.currentTimeMillis()
            )
        }
        
        try {
            // Kick the player
            val kickReason = "§c§lCsuXac Anti-Cheat\n§eViolation: ${violation.type}\n§cSeverity: ${violation.severity}"
            player.kickPlayer(kickReason)
            
            // Notify admins
            notifyAdmins(player, "KICKED", violation)
            
            return EnforcementResult(
                action = EnforcementAction.KICK,
                success = true,
                reason = kickReason,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to kick player ${player.name}: ${e.message}")
            return EnforcementResult(
                action = EnforcementAction.KICK,
                success = false,
                reason = "Error: ${e.message}",
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Execute warning action
     */
    private fun executeWarning(player: Player, violation: Violation): EnforcementResult {
        if (!config.warningMessages) {
            return EnforcementResult(
                action = EnforcementAction.WARNING,
                success = false,
                reason = "Warning messages are disabled in configuration",
                timestamp = System.currentTimeMillis()
            )
        }
        
        try {
            // Send warning message
            val warningMessage = when (violation.severity) {
                ViolationSeverity.HIGH -> "§c⚠️ Suspicious activity detected! This may result in action."
                ViolationSeverity.MEDIUM -> "§e⚠️ Unusual activity detected. Please check your connection."
                else -> "§6⚠️ Minor violation detected."
            }
            
            player.sendMessage(warningMessage)
            
            // Notify admins
            notifyAdmins(player, "WARNED", violation)
            
            return EnforcementResult(
                action = EnforcementAction.WARNING,
                success = true,
                reason = warningMessage,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to warn player ${player.name}: ${e.message}")
            return EnforcementResult(
                action = EnforcementAction.WARNING,
                success = false,
                reason = "Error: ${e.message}",
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Execute quarantine action
     */
    private fun executeQuarantine(player: Player, violation: Violation): EnforcementResult {
        if (!config.quarantineEnabled) {
            return EnforcementResult(
                action = EnforcementAction.QUARANTINE,
                success = false,
                reason = "Quarantine system is disabled in configuration",
                timestamp = System.currentTimeMillis()
            )
        }
        
        try {
            // Quarantine the player
            quarantinePlayer(player, "Automatic quarantine due to ${violation.type}")
            
            // Send quarantine message
            val quarantineMessage = "§4🚨 You have been quarantined for suspicious activity.\n§eReason: ${violation.type}"
            player.sendMessage(quarantineMessage)
            
            // Notify admins
            notifyAdmins(player, "QUARANTINED", violation)
            
            return EnforcementResult(
                action = EnforcementAction.QUARANTINE,
                success = true,
                reason = "Player quarantined",
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to quarantine player ${player.name}: ${e.message}")
            return EnforcementResult(
                action = EnforcementAction.QUARANTINE,
                success = false,
                reason = "Error: ${e.message}",
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Execute notify action
     */
    private fun executeNotify(player: Player, violation: Violation): EnforcementResult {
        try {
            // Send notification to player
            val notifyMessage = "§6ℹ️ Minor violation detected: ${violation.type}"
            player.sendMessage(notifyMessage)
            
            // Notify admins
            notifyAdmins(player, "NOTIFIED", violation)
            
            return EnforcementResult(
                action = EnforcementAction.NOTIFY,
                success = true,
                reason = notifyMessage,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to notify player ${player.name}: ${e.message}")
            return EnforcementResult(
                action = EnforcementAction.NOTIFY,
                success = false,
                reason = "Error: ${e.message}",
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Quarantine a player
     */
    private fun quarantinePlayer(player: Player, reason: String) {
        val playerId = player.uniqueId.toString()
        val quarantineInfo = QuarantineInfo(
            playerId = playerId,
            playerName = player.name,
            reason = reason,
            startTime = System.currentTimeMillis(),
            duration = config.quarantineDuration
        )
        
        quarantinedPlayers[playerId] = quarantineInfo
        
        // Log quarantine
        plugin.logger.warning("Player ${player.name} quarantined: $reason")
        
        // Notify all online admins
        Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
            if (onlinePlayer.hasPermission("csuxac.admin")) {
                onlinePlayer.sendMessage("§6§l[CsuXac] §ePlayer ${player.name} quarantined: $reason")
            }
        }
    }
    
    /**
     * Release player from quarantine
     */
    fun releaseFromQuarantine(playerId: String): Boolean {
        val quarantineInfo = quarantinedPlayers.remove(playerId)
        if (quarantineInfo != null) {
            plugin.logger.info("Player ${quarantineInfo.playerName} released from quarantine")
            
            // Reset violation count
            playerViolationCounts[playerId]?.set(0)
            
            return true
        }
        return false
    }
    
    /**
     * Check if player is quarantined
     */
    fun isQuarantined(playerId: String): Boolean {
        return quarantinedPlayers.containsKey(playerId)
    }
    
    /**
     * Get quarantine info
     */
    fun getQuarantineInfo(playerId: String): QuarantineInfo? {
        return quarantinedPlayers[playerId]
    }
    
    /**
     * Get all quarantined players
     */
    fun getQuarantinedPlayers(): List<QuarantineInfo> {
        return quarantinedPlayers.values.toList()
    }
    
    /**
     * Reset violation count for player
     */
    fun resetViolationCount(playerId: String) {
        playerViolationCounts[playerId]?.set(0)
    }
    
    /**
     * Get violation count for player
     */
    fun getViolationCount(playerId: String): Int {
        return playerViolationCounts[playerId]?.get() ?: 0
    }
    
    /**
     * Log violation
     */
    private fun logViolation(player: Player, violation: Violation, count: Int) {
        if (config.violationLogging) {
            plugin.logger.warning(
                "VIOLATION: ${player.name} (${player.uniqueId}) - " +
                "Type: ${violation.type}, Severity: ${violation.severity}, Count: $count"
            )
        }
    }
    
    /**
     * Notify admins
     */
    private fun notifyAdmins(player: Player, action: String, violation: Violation) {
        Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
            if (onlinePlayer.hasPermission("csuxac.admin")) {
                onlinePlayer.sendMessage(
                    "§6§l[CsuXac] §e$action ${player.name}: " +
                    "${violation.type} (${violation.severity})"
                )
            }
        }
    }
    
    /**
     * Update action history
     */
    private fun updateActionHistory(playerId: String, action: EnforcementAction, violation: Violation) {
        val history = playerActionHistory.getOrPut(playerId) { mutableListOf() }
        val enforcementAction = EnforcementActionRecord(
            action = action,
            violation = violation,
            timestamp = System.currentTimeMillis()
        )
        
        history.add(enforcementAction)
        
        // Keep only last 100 actions
        if (history.size > 100) {
            history.removeAt(0)
        }
    }
    
    /**
     * Get action history for player
     */
    fun getActionHistory(playerId: String): List<EnforcementActionRecord> {
        return playerActionHistory[playerId] ?: emptyList()
    }
    
    /**
     * Cleanup old data
     */
    fun cleanupOldData(maxAge: Long) {
        val currentTime = System.currentTimeMillis()
        
        // Cleanup old quarantines
        quarantinedPlayers.entries.removeIf { (_, info) ->
            currentTime - info.startTime > info.duration
        }
        
        // Cleanup old action history
        playerActionHistory.values.forEach { history ->
            history.removeAll { currentTime - it.timestamp > maxAge }
        }
    }
}

/**
 * Enforcement Action Types
 */
enum class EnforcementAction {
    BAN,
    KICK,
    WARNING,
    QUARANTINE,
    NOTIFY
}

/**
 * Enforcement Result
 */
data class EnforcementResult(
    val action: EnforcementAction,
    val success: Boolean,
    val reason: String,
    val timestamp: Long
)

/**
 * Quarantine Information
 */
data class QuarantineInfo(
    val playerId: String,
    val playerName: String,
    val reason: String,
    val startTime: Long,
    val duration: Long
)

/**
 * Enforcement Action Record
 */
data class EnforcementActionRecord(
    val action: EnforcementAction,
    val violation: Violation,
    val timestamp: Long
)