package com.csuxac.core.detection

import com.csuxac.core.models.*
import com.csuxac.core.physics.PhysicsSimulator
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reality Fork Detection (RFD) - Ultimate Enforcement Directive v5.0
 * 
 * Detects when cheat clients create different universes (forks) between
 * client and server. The system continuously compares the player's perceived
 * universe with the server's simulated universe.
 * 
 * If divergence is detected, the player's universe is forcibly synchronized
 * to the server's reality without notification.
 */
class RealityForkDetector(
    private val physicsSimulator: PhysicsSimulator
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Reality tracking per player
    private val playerRealityState = ConcurrentHashMap<String, RealityState>()
    private val realityDivergenceCount = ConcurrentHashMap<String, AtomicInteger>()
    private val lastRealitySync = ConcurrentHashMap<String, Long>()
    
    companion object {
        const val REALITY_CHECK_INTERVAL_MS = 50L // Check every 50ms
        const val MAX_DIVERGENCE_TOLERANCE = 0.1 // 10cm tolerance
        const val REALITY_SYNC_COOLDOWN_MS = 1000L // 1 second cooldown
        const val MAX_DIVERGENCE_BEFORE_QUARANTINE = 5
    }
    
    /**
     * Track player's reality state and detect forks
     */
    suspend fun trackReality(
        playerId: String,
        clientPosition: Vector3D,
        clientVelocity: Vector3D,
        clientEnvironment: ClientEnvironmentState,
        timestamp: Long
    ): RealityValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Get or create reality state
            val realityState = getOrCreateRealityState(playerId, clientPosition, timestamp)
            
            // Run server-side reality simulation
            val serverReality = simulateServerReality(realityState, timestamp)
            
            // Calculate reality divergence
            val positionDivergence = clientPosition.distanceTo(serverReality.position)
            val velocityDivergence = clientVelocity.distanceTo(serverReality.velocity)
            val environmentDivergence = calculateEnvironmentDivergence(clientEnvironment, serverReality.environment)
            
            val totalDivergence = positionDivergence + velocityDivergence + environmentDivergence
            
            // Check for reality fork
            if (totalDivergence > MAX_DIVERGENCE_TOLERANCE) {
                violations.add(createRealityForkViolation(
                    playerId, clientPosition, serverReality.position, totalDivergence
                ))
                confidence = 0.0
                
                // Increment divergence count
                val divergenceCount = realityDivergenceCount.computeIfAbsent(playerId) { AtomicInteger(0) }
                divergenceCount.incrementAndGet()
                
                logger.warn { 
                    "🌌 REALITY FORK DETECTED: Player $playerId - " +
                    "Client: $clientPosition, Server: ${serverReality.position}, " +
                    "Divergence: $totalDivergence"
                }
                
                // Force reality synchronization
                if (shouldForceRealitySync(playerId, timestamp)) {
                    forceRealitySync(playerId, serverReality)
                }
            } else {
                // Reset divergence count on successful reality alignment
                realityDivergenceCount[playerId]?.set(0)
            }
            
            // Update reality state
            updateRealityState(playerId, clientPosition, serverReality, timestamp)
            
            return RealityValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                clientReality = ClientReality(clientPosition, clientVelocity, clientEnvironment),
                serverReality = serverReality,
                divergence = totalDivergence,
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in reality fork detection for player $playerId" }
            violations.add(createRealityForkViolation(
                playerId, clientPosition, clientPosition, 0.0
            ))
            
            return RealityValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.0,
                clientReality = ClientReality(clientPosition, clientVelocity, clientEnvironment),
                serverReality = ServerReality(clientPosition, Vector3D.ZERO, EnvironmentState()),
                divergence = 0.0,
                timestamp = timestamp
            )
        }
    }
    
    /**
     * Simulate server-side reality for comparison
     */
    private suspend fun simulateServerReality(
        realityState: RealityState,
        timestamp: Long
    ): ServerReality {
        val deltaTime = timestamp - realityState.lastUpdate
        
        // Use simple physics simulation to predict where player should be
        val simulatedPosition = simulateSimplePhysics(
            realityState.position,
            realityState.velocity,
            deltaTime
        )
        
        // Calculate expected velocity based on physics
        val expectedVelocity = calculateExpectedVelocity(
            realityState.position,
            simulatedPosition,
            deltaTime
        )
        
        // Simulate environment changes
        val simulatedEnvironment = simulateEnvironmentChanges(
            realityState.environment,
            simulatedPosition,
            deltaTime
        )
        
        return ServerReality(
            position = simulatedPosition,
            velocity = expectedVelocity,
            environment = simulatedEnvironment
        )
    }
    
    /**
     * Force reality synchronization for a player
     */
    private fun forceRealitySync(playerId: String, serverReality: ServerReality) {
        val currentTime = System.currentTimeMillis()
        lastRealitySync[playerId] = currentTime
        
        // Update reality state to server values
        val realityState = playerRealityState[playerId]
        if (realityState != null) {
            playerRealityState[playerId] = realityState.copy(
                position = serverReality.position,
                velocity = serverReality.velocity,
                environment = serverReality.environment,
                lastUpdate = currentTime,
                lastSync = currentTime
            )
        }
        
        logger.info { "🔄 Reality synchronized for player $playerId to server state" }
    }
    
    /**
     * Check if reality sync should be forced
     */
    private fun shouldForceRealitySync(playerId: String, timestamp: Long): Boolean {
        val lastSync = lastRealitySync[playerId] ?: 0L
        val divergenceCount = realityDivergenceCount[playerId]?.get() ?: 0
        
        return (timestamp - lastSync) > REALITY_SYNC_COOLDOWN_MS && 
               divergenceCount >= MAX_DIVERGENCE_BEFORE_QUARANTINE
    }
    
    /**
     * Calculate environment divergence between client and server
     */
    private fun calculateEnvironmentDivergence(
        client: ClientEnvironmentState,
        server: EnvironmentState
    ): Double {
        var divergence = 0.0
        
        // Check ground state divergence
        if (client.isOnGround != server.isOnGround) divergence += 1.0
        
        // Check flying state divergence
        if (client.isFlying != server.isFlying) divergence += 1.0
        
        // Check sprinting state divergence
        if (client.isSprinting != server.isSprinting) divergence += 1.0
        
        // Check fluid state divergence
        if (client.isInFluid != server.isInFluid) divergence += 1.0
        
        return divergence
    }
    
    /**
     * Simple physics simulation for reality fork detection
     */
    private fun simulateSimplePhysics(
        position: Vector3D,
        velocity: Vector3D,
        deltaTime: Long
    ): Vector3D {
        if (deltaTime <= 0) return position
        
        val timeSeconds = deltaTime / 1000.0
        
        // Simple gravity simulation
        val gravity = Vector3D(0.0, -0.08, 0.0) // Minecraft gravity
        val newVelocity = velocity + gravity * timeSeconds
        
        // Apply velocity to position
        val newPosition = position + newVelocity * timeSeconds
        
        // Basic ground collision
        if (newPosition.y < 0) {
            return newPosition.copy(y = 0.0)
        }
        
        return newPosition
    }
    
    /**
     * Calculate expected velocity based on position change
     */
    private fun calculateExpectedVelocity(
        from: Vector3D,
        to: Vector3D,
        deltaTime: Long
    ): Vector3D {
        if (deltaTime <= 0) return Vector3D.ZERO
        
        val movement = to - from
        val timeSeconds = deltaTime / 1000.0
        
        return movement * (1.0 / timeSeconds)
    }
    
    /**
     * Simulate environment changes over time
     */
    private fun simulateEnvironmentChanges(
        current: EnvironmentState,
        position: Vector3D,
        deltaTime: Long
    ): EnvironmentState {
        // Simplified environment simulation
        // In a real implementation, this would check world data
        return current.copy(
            isOnGround = position.y <= 0.1, // Assume ground at y=0
            isInFluid = false // Simplified fluid detection
        )
    }
    
    /**
     * Get or create reality state for player
     */
    private fun getOrCreateRealityState(
        playerId: String,
        position: Vector3D,
        timestamp: Long
    ): RealityState {
        return playerRealityState.computeIfAbsent(playerId) {
            RealityState(
                playerId = playerId,
                position = position,
                velocity = Vector3D.ZERO,
                environment = EnvironmentState(),
                lastUpdate = timestamp,
                lastSync = timestamp
            )
        }
    }
    
    /**
     * Update reality state after validation
     */
    private fun updateRealityState(
        playerId: String,
        clientPosition: Vector3D,
        serverReality: ServerReality,
        timestamp: Long
    ) {
        val currentState = playerRealityState[playerId] ?: return
        
        playerRealityState[playerId] = currentState.copy(
            position = clientPosition,
            velocity = serverReality.velocity,
            environment = serverReality.environment,
            lastUpdate = timestamp
        )
    }
    
    /**
     * Create reality fork violation
     */
    private fun createRealityForkViolation(
        playerId: String,
        clientPosition: Vector3D,
        serverPosition: Vector3D,
        divergence: Double
    ): Violation {
        return Violation(
            type = ViolationType.REALITY_FORK,
            confidence = 0.95,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.REALITY_DIVERGENCE,
                    value = mapOf(
                        "clientPosition" to clientPosition,
                        "serverPosition" to serverPosition,
                        "divergence" to divergence,
                        "timestamp" to System.currentTimeMillis()
                    ),
                    confidence = 0.95,
                    description = "Reality fork detected: client-server universe divergence"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Cleanup player data
     */
    fun removePlayer(playerId: String) {
        playerRealityState.remove(playerId)
        realityDivergenceCount.remove(playerId)
        lastRealitySync.remove(playerId)
    }
    
    /**
     * Get reality divergence statistics
     */
    fun getRealityDivergenceStats(playerId: String): RealityDivergenceStats {
        val divergenceCount = realityDivergenceCount[playerId]?.get() ?: 0
        val lastSync = lastRealitySync[playerId] ?: 0L
        
        return RealityDivergenceStats(
            playerId = playerId,
            totalDivergences = divergenceCount,
            lastSyncTime = lastSync,
            isQuarantined = divergenceCount >= MAX_DIVERGENCE_BEFORE_QUARANTINE
        )
    }
}

/**
 * Reality state tracking for a player
 */
data class RealityState(
    val playerId: String,
    val position: Vector3D,
    val velocity: Vector3D,
    val environment: EnvironmentState,
    val lastUpdate: Long,
    val lastSync: Long
)

// ServerReality, ClientReality, and ClientEnvironmentState are now defined in the models package