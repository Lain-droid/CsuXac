package com.csuxac.core.models

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive Player Session Management System
 * Tracks all anti-cheat related data for each player
 */
data class PlayerSession(
    val playerId: String,
    val playerName: String,
    val uniqueId: String,
    val joinTime: Long = System.currentTimeMillis(),
    var lastSeen: Long = System.currentTimeMillis(),
    var lastPosition: Vector3D = Vector3D.ZERO,
    var lastVelocity: Vector3D = Vector3D.ZERO,
    var lastEnvironment: EnvironmentState = EnvironmentState(),
    
    // Violation tracking
    val violations: MutableList<Violation> = mutableListOf(),
    val violationCount: AtomicInteger = AtomicInteger(0),
    val totalViolations: AtomicInteger = AtomicInteger(0),
    
    // Performance metrics
    val totalMovements: AtomicLong = AtomicLong(0),
    val suspiciousMovements: AtomicLong = AtomicLong(0),
    val averageSuspicionScore: AtomicLong = AtomicLong(0),
    
    // Session state
    var isActive: Boolean = true,
    var isQuarantined: Boolean = false,
    var quarantineReason: String? = null,
    var quarantineStartTime: Long = 0L,
    
    // Anti-cheat specific data
    val physicsState: AdvancedPhysicsState = AdvancedPhysicsState(),
    val behaviorProfile: BehaviorProfile = BehaviorProfile(
        playerId = playerId,
        movementEntropy = 0.0,
        mouseEntropy = 0.0,
        timingVariance = 0.0,
        actionFrequencyDistribution = mutableMapOf(),
        totalSamples = 0,
        createdAt = System.currentTimeMillis(),
        lastUpdate = System.currentTimeMillis()
    ),
    
    // Timestamps for analysis
    val movementHistory: MutableList<MovementRecord> = mutableListOf(),
    val actionHistory: MutableList<ActionRecord> = mutableListOf(),
    val packetHistory: MutableList<PacketRecord> = mutableListOf()
) {
    
    /**
     * Update player position and movement data
     */
    fun updateMovement(
        position: Vector3D,
        velocity: Vector3D,
        environment: EnvironmentState,
        timestamp: Long
    ) {
        lastPosition = position
        lastVelocity = velocity
        lastEnvironment = environment
        lastSeen = timestamp
        totalMovements.incrementAndGet()
        
        // Add to movement history
        val movementRecord = MovementRecord(
            position = position,
            velocity = velocity,
            environment = environment,
            timestamp = timestamp
        )
        
        movementHistory.add(movementRecord)
        
        // Keep only last 1000 movements
        if (movementHistory.size > 1000) {
            movementHistory.removeAt(0)
        }
    }
    
    /**
     * Add violation to player session
     */
    fun addViolation(violation: Violation) {
        violations.add(violation)
        violationCount.incrementAndGet()
        totalViolations.incrementAndGet()
        
        // Update suspicion score
        val currentScore = averageSuspicionScore.get()
        val newScore = (currentScore + violation.type.severity.toLong()) / 2
        averageSuspicionScore.set(newScore)
        
        // Check for quarantine threshold
        if (violationCount.get() >= 5 && !isQuarantined) {
            quarantinePlayer("Multiple violations detected")
        }
    }
    
    /**
     * Quarantine player
     */
    fun quarantinePlayer(reason: String) {
        isQuarantined = true
        quarantineReason = reason
        quarantineStartTime = System.currentTimeMillis()
    }
    
    /**
     * Release player from quarantine
     */
    fun releaseFromQuarantine() {
        isQuarantined = false
        quarantineReason = null
        quarantineStartTime = 0L
        violationCount.set(0)
    }
    
    /**
     * Get current suspicion level
     */
    fun getSuspicionLevel(): SuspicionLevel {
        val score = averageSuspicionScore.get()
        return when {
            score < 1 -> SuspicionLevel.LOW
            score < 2 -> SuspicionLevel.MEDIUM
            score < 3 -> SuspicionLevel.HIGH
            else -> SuspicionLevel.CRITICAL
        }
    }
    
    /**
     * Get session statistics
     */
    fun getSessionStats(): SessionStats {
        return SessionStats(
            playerId = playerId,
            playerName = playerName,
            sessionDuration = System.currentTimeMillis() - joinTime,
            totalMovements = totalMovements.get(),
            suspiciousMovements = suspiciousMovements.get(),
            totalViolations = totalViolations.get(),
            currentViolations = violationCount.get().toLong(),
            averageSuspicionScore = averageSuspicionScore.get().toDouble(),
            suspicionLevel = getSuspicionLevel(),
            isQuarantined = isQuarantined,
            quarantineDuration = if (isQuarantined) System.currentTimeMillis() - quarantineStartTime else 0L
        )
    }
    
    /**
     * Cleanup old data
     */
    fun cleanupOldData(maxAge: Long) {
        val currentTime = System.currentTimeMillis()
        
        // Cleanup old movements
        movementHistory.removeAll { currentTime - it.timestamp > maxAge }
        
        // Cleanup old actions
        actionHistory.removeAll { currentTime - it.timestamp > maxAge }
        
        // Cleanup old packets
        packetHistory.removeAll { currentTime - it.timestamp > maxAge }
        
        // Cleanup old violations (keep last 100)
        if (violations.size > 100) {
            violations.removeAt(0)
        }
    }
}

/**
 * Movement Record for tracking player movements
 */
data class MovementRecord(
    val position: Vector3D,
    val velocity: Vector3D,
    val environment: EnvironmentState,
    val timestamp: Long
)

/**
 * Action Record for tracking player actions
 */
data class ActionRecord(
    val actionType: ActionType,
    val target: ActionTarget?,
    val position: Vector3D,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Session Statistics
 */
data class SessionStats(
    val playerId: String,
    val playerName: String,
    val sessionDuration: Long,
    val totalMovements: Long,
    val suspiciousMovements: Long,
    val totalViolations: Long,
    val currentViolations: Long,
    val averageSuspicionScore: Double,
    val suspicionLevel: SuspicionLevel,
    val isQuarantined: Boolean,
    val quarantineDuration: Long
)

/**
 * Suspicion Level Enum
 */
enum class SuspicionLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Player Session Manager
 */
class PlayerSessionManager {
    
    private val sessions = ConcurrentHashMap<String, PlayerSession>()
    private val sessionsByUniqueId = ConcurrentHashMap<String, PlayerSession>()
    
    /**
     * Create or get player session
     */
    fun getOrCreateSession(
        playerId: String,
        playerName: String,
        uniqueId: String
    ): PlayerSession {
        return sessions.getOrPut(playerId) {
            PlayerSession(
                playerId = playerId,
                playerName = playerName,
                uniqueId = uniqueId
            )
        }
    }
    
    /**
     * Get session by player ID
     */
    fun getSession(playerId: String): PlayerSession? {
        return sessions[playerId]
    }
    
    /**
     * Get session by unique ID
     */
    fun getSessionByUniqueId(uniqueId: String): PlayerSession? {
        return sessionsByUniqueId[uniqueId]
    }
    
    /**
     * Remove player session
     */
    fun removeSession(playerId: String): PlayerSession? {
        val session = sessions.remove(playerId)
        session?.let { sessionsByUniqueId.remove(it.uniqueId) }
        return session
    }
    
    /**
     * Get all active sessions
     */
    fun getActiveSessions(): List<PlayerSession> {
        return sessions.values.filter { it.isActive }
    }
    
    /**
     * Get quarantined players
     */
    fun getQuarantinedPlayers(): List<PlayerSession> {
        return sessions.values.filter { it.isQuarantined }
    }
    
    /**
     * Get session statistics
     */
    fun getSessionStats(): List<SessionStats> {
        return sessions.values.map { it.getSessionStats() }
    }
    
    /**
     * Cleanup old sessions
     */
    fun cleanupOldSessions(maxAge: Long) {
        val currentTime = System.currentTimeMillis()
        
        sessions.values.forEach { session ->
            if (currentTime - session.lastSeen > maxAge) {
                session.isActive = false
            }
            session.cleanupOldData(maxAge)
        }
    }
    
    /**
     * Get total statistics
     */
    fun getTotalStats(): TotalSessionStats {
        val activeSessions = getActiveSessions()
        val quarantinedPlayers = getQuarantinedPlayers()
        
        return TotalSessionStats(
            totalPlayers = activeSessions.size,
            quarantinedPlayers = quarantinedPlayers.size,
            totalViolations = activeSessions.sumOf { it.totalViolations.get() },
            averageSuspicionScore = if (activeSessions.isNotEmpty()) activeSessions.map { it.averageSuspicionScore.get().toDouble() }.average() else 0.0,
            totalMovements = activeSessions.sumOf { it.totalMovements.get() }
        )
    }
}

/**
 * Total Session Statistics
 */
data class TotalSessionStats(
    val totalPlayers: Int,
    val quarantinedPlayers: Int,
    val totalViolations: Long,
    val averageSuspicionScore: Double,
    val totalMovements: Long
)