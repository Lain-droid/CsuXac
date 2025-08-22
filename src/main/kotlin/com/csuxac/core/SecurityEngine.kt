package com.csuxac.core

import com.csuxac.config.SecurityConfig
import com.csuxac.core.models.*
import com.csuxac.core.detection.*
import com.csuxac.core.enforcement.*
import com.csuxac.core.monitoring.*
import com.csuxac.core.physics.PhysicsSimulator
import com.csuxac.core.packet.PacketFlowAnalyzer
import com.csuxac.core.adaptation.SelfEvolvingDefense
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * CsuXac Core Security Engine
 * 
 * The central orchestrator for the CsuXac anti-cheat system.
 * Manages all detection modules, enforcement actions, and system monitoring.
 */
class SecurityEngine(
    private val config: SecurityConfig
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Detection modules
    private val movementValidator = MovementValidator(config.movement)
    private val packetAnalyzer = PacketFlowAnalyzer(config.packet)
    private val physicsSimulator = PhysicsSimulator(config.physics)
    private val behaviorAnalyzer = BehaviorPatternAnalyzer(config.behavior)
    private val velocityEnforcer = VelocityEnforcer(config.velocity)
    private val challengeManager = ChallengeResponseManager(config.challenge)
    private val exploitDetector = ExploitSpecificDetector(config.exploits)
    private val adaptiveDefense = SelfEvolvingDefense(config.adaptation)
    
    // Enforcement modules
    private val violationHandler = ViolationHandler(config.enforcement)
    private val quarantineManager = QuarantineManager(config.quarantine)
    private val rollbackEngine = RollbackEngine(config.rollback)
    
    // Monitoring modules
    private val threatIntelligence = ThreatIntelligence(config.intelligence)
    private val performanceMonitor = PerformanceMonitor(config.performance)
    private val anomalyTracker = AnomalyTracker(config.anomaly)
    
    // Player session tracking
    private val playerSessions = ConcurrentHashMap<String, PlayerSecuritySession>()
    
    // Violation tracking with atomic counters for thread safety
    private val violationCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val suspicionLevels = ConcurrentHashMap<String, AtomicInteger>()
    
    private var isRunning = false
    
    /**
     * Start the Security Engine and all detection modules
     */
    suspend fun start() {
        if (isRunning) return
        
        logger.info { "🚀 Starting CsuXac Core Security Engine..." }
        
        // Initialize all modules
        logger.info { "📋 Initializing detection modules..." }
        
        logger.info { "🛡️ Initializing enforcement modules..." }
        
        logger.info { "📊 Initializing monitoring modules..." }
        
        // Start adaptive defense
        scope.launch {
            try {
                adaptiveDefense.evolveDefense()
                logger.info { "🔧 Adaptive defense system initialized" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to start adaptive defense" }
            }
        }
        
        isRunning = true
        logger.info { "✅ CsuXac Core Security Engine started successfully" }
    }
    
    /**
     * Stop the Security Engine and all modules
     */
    suspend fun stop() {
        if (!isRunning) return
        
        logger.info { "🛑 Stopping CsuXac Core Security Engine..." }
        
        // Stop adaptive defense
        logger.info { "🔧 Adaptive defense system stopped" }
        
        // Cancel all coroutines
        scope.cancel()
        
        isRunning = false
        logger.info { "✅ CsuXac Core Security Engine stopped successfully" }
    }
    
    /**
     * Process player movement and detect violations
     */
    suspend fun processMovement(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        timestamp: Long
    ) {
        if (!isRunning) return
        
        try {
            // Validate movement
            val result = movementValidator.validateMovement(from, to, timestamp)
            if (result.isValid) {
                logger.debug { "Valid movement for $playerId: $from -> $to" }
                return
            }
            
            // Handle violation
            handleViolation(playerId,                     ViolationType.MOVEMENT_HACK, result)
            
        } catch (e: Exception) {
            logger.error(e) { "Error processing movement for $playerId" }
        }
    }
    
    /**
     * Process player action and detect violations
     */
    suspend fun processAction(
        playerId: String,
        action: PlayerAction,
        timestamp: Long
    ) {
        if (!isRunning) return
        
        try {
            // Analyze behavior patterns
            val session = getOrCreateSession(playerId)
            val result = behaviorAnalyzer.analyzeAction(action, session)
            if (result.isValid) {
                logger.debug { "Valid action for $playerId: ${action.type}" }
                return
            }
            
            // Handle violation
            handleViolation(playerId,                     ViolationType.BEHAVIOR_HACK, result)
            
        } catch (e: Exception) {
            logger.error(e) { "Error processing action for $playerId" }
        }
    }
    
    /**
     * Process velocity validation
     */
    suspend fun processVelocity(
        playerId: String,
        expected: Vector3D,
        actual: Vector3D,
        timestamp: Long
    ) {
        if (!isRunning) return
        
        try {
            val session = getOrCreateSession(playerId)
            val result = velocityEnforcer.validateVelocity(expected, actual, session)
            if (result.isValid) {
                return
            }
            
            handleViolation(playerId,                     ViolationType.VELOCITY_HACK, result)
            
        } catch (e: Exception) {
            logger.error(e) { "Error processing velocity for $playerId" }
        }
    }
    
    /**
     * Handle detected violation with escalating punishment system
     */
    private suspend fun handleViolation(
        playerId: String,
        type: ViolationType,
        evidence: Any
    ) {
        val violationCount = violationCounts.getOrPut(playerId) { AtomicInteger(0) }
        val suspicionLevel = suspicionLevels.getOrPut(playerId) { AtomicInteger(0) }
        
        violationCount.incrementAndGet()
        suspicionLevel.addAndGet(type.severity)
        
        logger.warn { "🚨 VIOLATION DETECTED: Player $playerId - $type (Level: ${suspicionLevel.get()})" }
        
        when {
            suspicionLevel.get() >= 100 -> {
                violationHandler.permanentBan(playerId, type, evidence)
                logger.error { "💀 PERMANENT BAN: Player $playerId - Multiple violations detected" }
            }
            suspicionLevel.get() >= 50 -> {
                violationHandler.temporaryBan(playerId, type, evidence, 24 * 60 * 60 * 1000) // 24 hours
                logger.warn { "⏰ TEMPORARY BAN: Player $playerId - Suspicious behavior" }
            }
            suspicionLevel.get() >= 25 -> {
                quarantineManager.quarantinePlayer(playerId, type, evidence)
                logger.warn { "🔒 QUARANTINE: Player $playerId - Under investigation" }
            }
            else -> {
                violationHandler.issueWarning(playerId, type, evidence)
                logger.info { "⚠️ WARNING: Player $playerId - Minor violation" }
            }
        }
        
        threatIntelligence.recordViolation(playerId, type, evidence)
    }
    
    /**
     * Get player security session
     */
    fun getPlayerSession(playerId: String): PlayerSecuritySession? {
        return playerSessions[playerId]
    }
    
    /**
     * Get or create a player security session
     */
    private fun getOrCreateSession(playerId: String): PlayerSecuritySession {
        return playerSessions.computeIfAbsent(playerId) {
            PlayerSecuritySession(
                playerId = playerId,
                startTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Get comprehensive security metrics
     */
    suspend fun getSecurityMetrics(): Map<String, Any> {
        return mapOf(
            "totalPlayers" to playerSessions.size,
            "activeViolations" to violationCounts.size,
            "averageSuspicionLevel" to suspicionLevels.values.map { it.get() }.average(),
            "isRunning" to isRunning
        )
    }
    
    fun isRunning(): Boolean = isRunning
}