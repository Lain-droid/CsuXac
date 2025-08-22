package com.csuxac.core.enforcement

import com.csuxac.config.EnforcementConfig
import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * ViolationHandler - Zero tolerance violation enforcement system
 * 
 * Features:
 * - Immediate action on violations
 * - Escalating punishment system
 * - Evidence collection and logging
 * - Permanent ban enforcement
 * - Violation history tracking
 */
class ViolationHandler(
    private val config: EnforcementConfig
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Violation tracking
    private val violationHistory = ConcurrentHashMap<String, MutableList<ViolationRecord>>()
    private val playerPunishments = ConcurrentHashMap<String, PlayerPunishment>()
    private val banList = ConcurrentHashMap<String, BanRecord>()
    
    /**
     * Issue warning for minor violations
     */
    suspend fun issueWarning(playerId: String, type: ViolationType, evidence: Any) {
        logger.warn { "⚠️ WARNING issued to $playerId for $type" }
        
        val warning = ViolationRecord(
            playerId = playerId,
            type = type,
            evidence = evidence,
            timestamp = System.currentTimeMillis(),
            action = ViolationAction.WARNING
        )
        
        recordViolation(warning)
        updatePlayerPunishment(playerId, type, ViolationAction.WARNING)
    }
    
    /**
     * Quarantine player for investigation
     */
    suspend fun quarantinePlayer(playerId: String, type: ViolationType, evidence: Any) {
        logger.warn { "🔒 QUARANTINE initiated for $playerId due to $type" }
        
        val quarantine = ViolationRecord(
            playerId = playerId,
            type = type,
            evidence = evidence,
            timestamp = System.currentTimeMillis(),
            action = ViolationAction.QUARANTINE
        )
        
        recordViolation(quarantine)
        updatePlayerPunishment(playerId, type, ViolationAction.QUARANTINE)
        
        // Notify quarantine manager
        // quarantineManager.quarantinePlayer(playerId, type, evidence)
    }
    
    /**
     * Issue temporary ban
     */
    suspend fun temporaryBan(playerId: String, type: ViolationType, evidence: Any, duration: Long) {
        logger.warn { "⏰ TEMPORARY BAN issued to $playerId for $type (${duration / 1000 / 60} minutes)" }
        
        val tempBan = ViolationRecord(
            playerId = playerId,
            type = type,
            evidence = evidence,
            timestamp = System.currentTimeMillis(),
            action = ViolationAction.TEMPORARY_BAN,
            duration = duration
        )
        
        recordViolation(tempBan)
        updatePlayerPunishment(playerId, type, ViolationAction.TEMPORARY_BAN)
        
        // Add to ban list
        val banRecord = BanRecord(
            playerId = playerId,
            type = BanType.TEMPORARY,
            reason = type.description,
            evidence = evidence,
            issuedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + duration,
            issuedBy = "CsuXac Core"
        )
        
        banList[playerId] = banRecord
    }
    
    /**
     * Issue permanent ban - zero tolerance
     */
    suspend fun permanentBan(playerId: String, type: ViolationType, evidence: Any) {
        logger.error { "💀 PERMANENT BAN issued to $playerId for $type - ZERO TOLERANCE" }
        
        val permBan = ViolationRecord(
            playerId = playerId,
            type = type,
            evidence = evidence,
            timestamp = System.currentTimeMillis(),
            action = ViolationAction.PERMANENT_BAN
        )
        
        recordViolation(permBan)
        updatePlayerPunishment(playerId, type, ViolationAction.PERMANENT_BAN)
        
        // Add to permanent ban list
        val banRecord = BanRecord(
            playerId = playerId,
            type = BanType.PERMANENT,
            reason = type.description,
            evidence = evidence,
            issuedAt = System.currentTimeMillis(),
            expiresAt = Long.MAX_VALUE, // Never expires
            issuedBy = "CsuXac Core"
        )
        
        banList[playerId] = banRecord
        
        // Log for audit
        logBanAudit(banRecord)
    }
    
    /**
     * Check if player is banned
     */
    fun isPlayerBanned(playerId: String): Boolean {
        val banRecord = banList[playerId] ?: return false
        
        return when (banRecord.type) {
            BanType.PERMANENT -> true
            BanType.TEMPORARY -> System.currentTimeMillis() < banRecord.expiresAt
        }
    }
    
    /**
     * Get player's ban record
     */
    fun getBanRecord(playerId: String): BanRecord? {
        return banList[playerId]
    }
    
    /**
     * Get player's violation history
     */
    fun getViolationHistory(playerId: String): List<ViolationRecord> {
        return violationHistory[playerId] ?: emptyList()
    }
    
    /**
     * Get player's current punishment status
     */
    fun getPlayerPunishment(playerId: String): PlayerPunishment? {
        return playerPunishments[playerId]
    }
    
    /**
     * Check if player should be punished
     */
    fun shouldPunishPlayer(playerId: String, violationCount: Int): Boolean {
        if (!config.zeroTolerance) return false
        
        val punishment = playerPunishments[playerId]
        if (punishment == null) return violationCount >= config.warningThreshold
        
        return when (punishment.currentLevel) {
            PunishmentLevel.WARNING -> violationCount >= config.quarantineThreshold
            PunishmentLevel.QUARANTINE -> violationCount >= config.tempBanThreshold
            PunishmentLevel.TEMPORARY_BAN -> violationCount >= config.permanentBanThreshold
            PunishmentLevel.PERMANENT_BAN -> true
        }
    }
    
    /**
     * Get recommended action for violation count
     */
    fun getRecommendedAction(violationCount: Int): ViolationAction {
        return when {
            violationCount >= config.permanentBanThreshold -> ViolationAction.PERMANENT_BAN
            violationCount >= config.tempBanThreshold -> ViolationAction.TEMPORARY_BAN
            violationCount >= config.quarantineThreshold -> ViolationAction.QUARANTINE
            violationCount >= config.warningThreshold -> ViolationAction.WARNING
            else -> ViolationAction.NONE
        }
    }
    
    /**
     * Record violation for tracking
     */
    private fun recordViolation(violation: ViolationRecord) {
        val history = violationHistory.getOrPut(violation.playerId) { mutableListOf() }
        history.add(violation)
        
        // Keep only recent violations
        if (history.size > config.maxViolations) {
            history.removeAt(0)
        }
        
        // Log violation
        logViolation(violation)
    }
    
    /**
     * Update player's punishment status
     */
    private fun updatePlayerPunishment(playerId: String, type: ViolationType, action: ViolationAction) {
        val punishment = playerPunishments.getOrPut(playerId) { PlayerPunishment(playerId) }
        
        punishment.violationCount++
        punishment.lastViolation = System.currentTimeMillis()
        punishment.lastViolationType = type
        punishment.currentLevel = getPunishmentLevel(action)
        punishment.punishmentHistory.add(action)
        
        // Clean up old punishments
        if (punishment.punishmentHistory.size > 10) {
            punishment.punishmentHistory.removeAt(0)
        }
    }
    
    /**
     * Get punishment level from action
     */
    private fun getPunishmentLevel(action: ViolationAction): PunishmentLevel {
        return when (action) {
            ViolationAction.NONE -> PunishmentLevel.NONE
            ViolationAction.WARNING -> PunishmentLevel.WARNING
            ViolationAction.QUARANTINE -> PunishmentLevel.QUARANTINE
            ViolationAction.TEMPORARY_BAN -> PunishmentLevel.TEMPORARY_BAN
            ViolationAction.PERMANENT_BAN -> PunishmentLevel.PERMANENT_BAN
        }
    }
    
    /**
     * Log violation for audit
     */
    private fun logViolation(violation: ViolationRecord) {
        logger.info {
            "🚨 VIOLATION RECORDED: Player: ${violation.playerId}, " +
            "Type: ${violation.type}, Action: ${violation.action}, " +
            "Evidence: ${violation.evidence}"
        }
    }
    
    /**
     * Log ban audit
     */
    private fun logBanAudit(banRecord: BanRecord) {
        logger.error {
            "💀 BAN AUDIT: Player: ${banRecord.playerId}, " +
            "Type: ${banRecord.type}, Reason: ${banRecord.reason}, " +
            "Issued by: ${banRecord.issuedBy}, Evidence: ${banRecord.evidence}"
        }
    }
    
    /**
     * Clean up expired temporary bans
     */
    suspend fun cleanupExpiredBans() {
        val now = System.currentTimeMillis()
        val expiredBans = banList.entries.filter { (_, ban) ->
            ban.type == BanType.TEMPORARY && ban.expiresAt < now
        }
        
        expiredBans.forEach { (playerId, _) ->
            banList.remove(playerId)
            logger.info { "✅ Temporary ban expired for $playerId" }
        }
        
        if (expiredBans.isNotEmpty()) {
            logger.info { "🧹 Cleaned up ${expiredBans.size} expired temporary bans" }
        }
    }
    
    /**
     * Get system statistics
     */
    fun getSystemStats(): EnforcementStats {
        return EnforcementStats(
            totalViolations = violationHistory.values.sumOf { it.size },
            activeBans = banList.size,
            permanentBans = banList.values.count { it.type == BanType.PERMANENT },
            temporaryBans = banList.values.count { it.type == BanType.TEMPORARY },
            quarantinedPlayers = playerPunishments.values.count { it.currentLevel == PunishmentLevel.QUARANTINE }
        )
    }
    
    // Data classes
    data class ViolationRecord(
        val playerId: String,
        val type: ViolationType,
        val evidence: Any,
        val timestamp: Long,
        val action: ViolationAction,
        val duration: Long? = null
    )
    
    data class PlayerPunishment(
        val playerId: String,
        var violationCount: Int = 0,
        var lastViolation: Long = 0,
        var lastViolationType: ViolationType? = null,
        var currentLevel: PunishmentLevel = PunishmentLevel.NONE,
        val punishmentHistory: MutableList<ViolationAction> = mutableListOf()
    )
    
    data class BanRecord(
        val playerId: String,
        val type: BanType,
        val reason: String,
        val evidence: Any,
        val issuedAt: Long,
        val expiresAt: Long,
        val issuedBy: String
    )
    
    data class EnforcementStats(
        val totalViolations: Int,
        val activeBans: Int,
        val permanentBans: Int,
        val temporaryBans: Int,
        val quarantinedPlayers: Int
    )
    
    enum class ViolationAction {
        NONE, WARNING, QUARANTINE, TEMPORARY_BAN, PERMANENT_BAN
    }
    
    enum class PunishmentLevel {
        NONE, WARNING, QUARANTINE, TEMPORARY_BAN, PERMANENT_BAN
    }
    
    enum class BanType {
        TEMPORARY, PERMANENT
    }
}