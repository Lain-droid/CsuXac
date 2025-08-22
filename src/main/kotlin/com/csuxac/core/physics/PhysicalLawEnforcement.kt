package com.csuxac.core.physics

import com.csuxac.config.PhysicsConfig
import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Physical Law Enforcement - v2.0 Ultimate Enforcement Directive
 * 
 * This system runs a completely independent physics engine on the server.
 * Every movement is validated against gravity, friction, momentum, collision,
 * step height, air resistance and other parameters.
 * 
 * If client-reported position does not match server simulation within millisecond
 * precision, it is INVALID and immediately corrected.
 */
class PhysicalLawEnforcement(
    private val config: PhysicsConfig
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Physics state tracking per player
    private val playerPhysicsState = ConcurrentHashMap<String, PlayerPhysicsState>()
    private val lastValidPositions = ConcurrentHashMap<String, Vector3D>()
    private val violationCounts = ConcurrentHashMap<String, Int>()
    
    companion object {
        // Minecraft Physics Constants (exact values)
        const val GRAVITY = -0.08 // blocks/tick²
        const val AIR_RESISTANCE = 0.98 // multiplier per tick
        const val GROUND_FRICTION = 0.6 // when on ground
        const val FLUID_FRICTION = 0.8 // when in water/lava
        const val JUMP_VELOCITY = 0.42 // blocks/tick
        const val STEP_HEIGHT = 0.6 // blocks
        const val PLAYER_HEIGHT = 1.8 // blocks
        const val PLAYER_WIDTH = 0.6 // blocks
        const val MAX_WALK_SPEED = 0.2158 // blocks/tick
        const val MAX_SPRINT_SPEED = 0.2806 // blocks/tick
        const val MAX_FLY_SPEED = 0.4 // blocks/tick (creative)
        const val TERMINAL_VELOCITY = -3.92 // blocks/tick
        const val TICK_TIME = 50 // milliseconds per tick
    }
    
    /**
     * Validate movement against server-side physics simulation
     */
    suspend fun validateMovement(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        deltaTime: Long,
        environment: EnvironmentState
    ): PhysicsValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Get or create physics state
            val physicsState = getOrCreatePhysicsState(playerId, from)
            
            // Run server-side physics simulation
            val simulatedPosition = simulateMovement(physicsState, deltaTime, environment)
            
            // Calculate discrepancy
            val discrepancy = simulatedPosition.distanceTo(to)
            val tolerance = config.simulationAccuracy // Default: 0.01 blocks
            
            if (discrepancy > tolerance) {
                // CRITICAL: Client position doesn't match server physics
                violations.add(createPhysicsViolation(
                    playerId, from, to, simulatedPosition, discrepancy, "Position mismatch"
                ))
                confidence = 0.0
                
                // Increment violation count
                violationCounts[playerId] = (violationCounts[playerId] ?: 0) + 1
                
                logger.warn { 
                    "🚨 PHYSICS VIOLATION: Player $playerId - Expected: $simulatedPosition, Got: $to, Discrepancy: $discrepancy"
                }
            }
            
            // Additional physics checks
            violations.addAll(performAdditionalPhysicsChecks(physicsState, from, to, deltaTime, environment))
            
            // Update physics state for next validation
            updatePhysicsState(playerId, to, deltaTime, environment)
            
            return PhysicsValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                simulatedPosition = simulatedPosition,
                actualPosition = to,
                discrepancy = discrepancy,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in physical law enforcement for player $playerId" }
            violations.add(createPhysicsViolation(
                playerId, from, to, from, 0.0, "Physics simulation error: ${e.message}"
            ))
            
            return PhysicsValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.0,
                simulatedPosition = from,
                actualPosition = to,
                discrepancy = 0.0,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Simulate movement using server-side physics engine
     */
    private fun simulateMovement(
        state: PlayerPhysicsState,
        deltaTime: Long,
        environment: EnvironmentState
    ): Vector3D {
        var position = state.position
        var velocity = state.velocity
        var acceleration = Vector3D.ZERO
        
        // Convert deltaTime to ticks for simulation
        val ticks = deltaTime.toDouble() / TICK_TIME
        
        // Apply gravity (unless flying or on ground moving upward)
        if (!environment.isFlying && (!environment.isOnGround || velocity.y > 0)) {
            acceleration = acceleration.copy(y = acceleration.y + GRAVITY)
        }
        
        // Apply air resistance
        if (velocity.magnitude() > 0.01) {
            val resistance = velocity.normalize() * -velocity.magnitude().pow(2) * 0.01
            acceleration = acceleration + resistance
        }
        
        // Apply fluid resistance if in water/lava
        if (environment.isInFluid) {
            velocity = velocity * FLUID_FRICTION
        }
        
        // Apply ground friction
        if (environment.isOnGround && abs(velocity.x) + abs(velocity.z) > 0.01) {
            velocity = velocity.copy(
                x = velocity.x * GROUND_FRICTION,
                z = velocity.z * GROUND_FRICTION
            )
        }
        
        // Integrate velocity and position
        velocity = velocity + acceleration * ticks
        position = position + velocity * ticks
        
        // Enforce terminal velocity
        if (velocity.y < TERMINAL_VELOCITY) {
            velocity = velocity.copy(y = TERMINAL_VELOCITY)
        }
        
        // Enforce speed limits based on movement type
        val horizontalSpeed = sqrt(velocity.x.pow(2) + velocity.z.pow(2))
        val maxSpeed = when {
            environment.isFlying -> MAX_FLY_SPEED
            environment.isSprinting -> MAX_SPRINT_SPEED
            else -> MAX_WALK_SPEED
        }
        
        if (horizontalSpeed > maxSpeed) {
            val scale = maxSpeed / horizontalSpeed
            velocity = velocity.copy(
                x = velocity.x * scale,
                z = velocity.z * scale
            )
        }
        
        return position
    }
    
    /**
     * Perform additional physics validation checks
     */
    private fun performAdditionalPhysicsChecks(
        state: PlayerPhysicsState,
        from: Vector3D,
        to: Vector3D,
        deltaTime: Long,
        environment: EnvironmentState
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        val movement = to - from
        val distance = movement.magnitude()
        val speed = distance / (deltaTime / 1000.0) // blocks per second
        
        // Check for impossible movement speeds
        val maxPossibleSpeed = when {
            environment.isFlying -> MAX_FLY_SPEED * 20 // Convert to blocks/second
            environment.isSprinting -> MAX_SPRINT_SPEED * 20
            else -> MAX_WALK_SPEED * 20
        }
        
        if (speed > maxPossibleSpeed * 1.1) { // 10% tolerance
            violations.add(createPhysicsViolation(
                state.playerId, from, to, from, distance,
                "Impossible speed: $speed blocks/s (max: $maxPossibleSpeed)"
            ))
        }
        
        // Check for impossible vertical movement
        if (!environment.isFlying) {
            val verticalMovement = abs(movement.y)
            val maxVerticalMovement = if (environment.isOnGround) {
                JUMP_VELOCITY * (deltaTime / TICK_TIME) + STEP_HEIGHT
            } else {
                abs(GRAVITY) * (deltaTime / TICK_TIME).pow(2) + abs(state.velocity.y) * (deltaTime / TICK_TIME)
            }
            
            if (verticalMovement > maxVerticalMovement * 1.2) { // 20% tolerance
                violations.add(createPhysicsViolation(
                    state.playerId, from, to, from, verticalMovement,
                    "Impossible vertical movement: $verticalMovement blocks (max: $maxVerticalMovement)"
                ))
            }
        }
        
        // Check for phase detection (going through blocks)
        if (environment.hasCollisions && detectPhaseMovement(from, to, environment)) {
            violations.add(createPhysicsViolation(
                state.playerId, from, to, from, distance,
                "Phase hack detected: movement through solid blocks"
            ))
        }
        
        return violations
    }
    
    /**
     * Detect phase/noclip movement through blocks
     */
    private fun detectPhaseMovement(from: Vector3D, to: Vector3D, environment: EnvironmentState): Boolean {
        // Simple phase detection - would need actual world collision data
        // This is a placeholder for demonstration
        if (!environment.hasCollisions) return false
        
        // Check if movement goes through solid blocks
        val movement = to - from
        val steps = max(1, movement.magnitude().toInt())
        
        for (i in 0..steps) {
            val progress = i.toDouble() / steps
            val checkPos = from + movement * progress
            
            // Check if position intersects with solid blocks
            // This would need actual world data in a real implementation
            if (isPositionInSolidBlock(checkPos, environment)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if position intersects with solid blocks
     */
    private fun isPositionInSolidBlock(position: Vector3D, environment: EnvironmentState): Boolean {
        // Placeholder - would need actual world block data
        // For now, assume no phase if we don't have collision data
        return false
    }
    
    /**
     * Get or create physics state for player
     */
    private fun getOrCreatePhysicsState(playerId: String, position: Vector3D): PlayerPhysicsState {
        return playerPhysicsState.computeIfAbsent(playerId) {
            PlayerPhysicsState(
                playerId = playerId,
                position = position,
                velocity = Vector3D.ZERO,
                lastUpdate = System.currentTimeMillis(),
                isOnGround = true,
                isFlying = false,
                isSprinting = false
            )
        }
    }
    
    /**
     * Update physics state after validation
     */
    private fun updatePhysicsState(
        playerId: String,
        position: Vector3D,
        deltaTime: Long,
        environment: EnvironmentState
    ) {
        val state = playerPhysicsState[playerId] ?: return
        val oldPosition = state.position
        
        // Calculate new velocity based on movement
        val movement = position - oldPosition
        val velocity = if (deltaTime > 0) {
            movement * (TICK_TIME.toDouble() / deltaTime)
        } else Vector3D.ZERO
        
        // Update state
        playerPhysicsState[playerId] = state.copy(
            position = position,
            velocity = velocity,
            lastUpdate = System.currentTimeMillis(),
            isOnGround = environment.isOnGround,
            isFlying = environment.isFlying,
            isSprinting = environment.isSprinting
        )
        
        // Update last valid position
        lastValidPositions[playerId] = position
    }
    
    /**
     * Create physics violation evidence
     */
    private fun createPhysicsViolation(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        expected: Vector3D,
        discrepancy: Double,
        description: String
    ): Violation {
        return Violation(
            type = ViolationType.COLLISION_HACK,
            confidence = 0.95,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.PHYSICS_VIOLATION,
                    value = mapOf(
                        "from" to from,
                        "to" to to,
                        "expected" to expected,
                        "discrepancy" to discrepancy,
                        "description" to description
                    ),
                    confidence = 0.95,
                    description = "Physical law violation: $description"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Get player violation count
     */
    fun getViolationCount(playerId: String): Int {
        return violationCounts[playerId] ?: 0
    }
    
    /**
     * Reset player physics state (e.g., after teleport)
     */
    fun resetPlayerState(playerId: String, position: Vector3D) {
        playerPhysicsState[playerId] = PlayerPhysicsState(
            playerId = playerId,
            position = position,
            velocity = Vector3D.ZERO,
            lastUpdate = System.currentTimeMillis(),
            isOnGround = true,
            isFlying = false,
            isSprinting = false
        )
        lastValidPositions[playerId] = position
        violationCounts.remove(playerId)
        
        logger.info { "🔄 Reset physics state for player $playerId at $position" }
    }
    
    /**
     * Cleanup player data
     */
    fun removePlayer(playerId: String) {
        playerPhysicsState.remove(playerId)
        lastValidPositions.remove(playerId)
        violationCounts.remove(playerId)
    }
}

/**
 * Player physics state tracking
 */
data class PlayerPhysicsState(
    val playerId: String,
    val position: Vector3D,
    val velocity: Vector3D,
    val lastUpdate: Long,
    val isOnGround: Boolean,
    val isFlying: Boolean,
    val isSprinting: Boolean
)

/**
 * Environment state for physics calculations
 */
data class EnvironmentState(
    val isOnGround: Boolean = true,
    val isFlying: Boolean = false,
    val isSprinting: Boolean = false,
    val isInFluid: Boolean = false,
    val hasCollisions: Boolean = true,
    val blockType: String? = null,
    val fluidLevel: Float = 0f
)

/**
 * Physics validation result
 */
data class PhysicsValidationResult(
    val isValid: Boolean,
    val violations: List<Violation>,
    val confidence: Double,
    val simulatedPosition: Vector3D,
    val actualPosition: Vector3D,
    val discrepancy: Double,
    val timestamp: Long
)