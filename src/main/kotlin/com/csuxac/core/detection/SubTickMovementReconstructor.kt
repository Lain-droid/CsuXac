package com.csuxac.core.detection

import com.csuxac.config.MovementConfig
import com.csuxac.core.models.*
import com.csuxac.core.physics.PhysicalLawEnforcement
import com.csuxac.core.physics.EnvironmentState
import com.csuxac.util.logging.defaultLogger
import kotlin.math.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Sub-Tick Movement Reconstruction (SMR) - v2.0 Ultimate Enforcement Directive
 * 
 * This system divides each server tick into 100+ sub-time slices to reconstruct
 * movement between ticks. Each sub-tick applies:
 * - Gravity 
 * - Horizontal speed reduction (friction)
 * - Collision checks
 * 
 * If client sends position that doesn't match sub-tick reconstruction,
 * it is marked as "impossible movement".
 */
class SubTickMovementReconstructor(
    private val config: MovementConfig
) {
    private val logger = defaultLogger()
    
    // Sub-tick tracking per player
    private val playerSubTickHistory = ConcurrentHashMap<String, MutableList<SubTickFrame>>()
    private val lastTickPositions = ConcurrentHashMap<String, Vector3D>()
    private val interpolationErrors = ConcurrentHashMap<String, Double>()
    
    companion object {
        const val SUB_TICKS_PER_TICK = 100 // Divide each tick into 100 sub-ticks
        const val TICK_DURATION_MS = 50.0 // 50ms per tick
        const val SUB_TICK_DURATION_MS = TICK_DURATION_MS / SUB_TICKS_PER_TICK // 0.5ms per sub-tick
        const val RECONSTRUCTION_TOLERANCE = 0.005 // 5mm tolerance
    }
    
    /**
     * Reconstruct movement using sub-tick interpolation
     */
    suspend fun reconstructMovement(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        deltaTime: Long,
        environment: EnvironmentState
    ): SubTickValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Calculate number of sub-ticks for this movement
            val numSubTicks = max(1, (deltaTime / SUB_TICK_DURATION_MS).toInt())
            
            // Perform sub-tick reconstruction
            val reconstructedPath = performSubTickReconstruction(
                from, to, numSubTicks, environment
            )
            
            // Calculate interpolation error
            val interpolationError = calculateInterpolationError(reconstructedPath, to)
            interpolationErrors[playerId] = interpolationError
            
            // Check if movement matches sub-tick reconstruction
            if (interpolationError > RECONSTRUCTION_TOLERANCE) {
                violations.add(createSubTickViolation(
                    playerId, from, to, reconstructedPath.last(), interpolationError,
                    "Sub-tick reconstruction mismatch"
                ))
                confidence = 0.0
                
                logger.warn {
                    "🔬 SUB-TICK VIOLATION: Player $playerId - " +
                    "Expected: ${reconstructedPath.last()}, Got: $to, " +
                    "Error: $interpolationError blocks"
                }
            }
            
            // Additional sub-tick checks
            violations.addAll(performAdvancedSubTickChecks(
                playerId, from, to, reconstructedPath, deltaTime, environment
            ))
            
            // Update tracking data
            updateSubTickHistory(playerId, from, to, reconstructedPath, deltaTime)
            
            return SubTickValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                reconstructedPath = reconstructedPath,
                actualPosition = to,
                interpolationError = interpolationError,
                numSubTicks = numSubTicks,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in sub-tick reconstruction for player $playerId" }
            violations.add(createSubTickViolation(
                playerId, from, to, from, 0.0,
                "Sub-tick reconstruction error: ${e.message}"
            ))
            
            return SubTickValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.0,
                reconstructedPath = listOf(from, to),
                actualPosition = to,
                interpolationError = 0.0,
                numSubTicks = 1,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Perform detailed sub-tick movement reconstruction
     */
    private fun performSubTickReconstruction(
        from: Vector3D,
        to: Vector3D,
        numSubTicks: Int,
        environment: EnvironmentState
    ): List<Vector3D> {
        val path = mutableListOf<Vector3D>()
        var currentPosition = from
        
        // Calculate initial velocity from movement
        val totalMovement = to - from
        var currentVelocity = totalMovement * (1.0 / numSubTicks)
        
        path.add(currentPosition)
        
        for (subTick in 1..numSubTicks) {
            val progress = subTick.toDouble() / numSubTicks
            
            // Apply gravity in sub-tick increments
            if (!environment.isFlying && !environment.isOnGround) {
                currentVelocity = currentVelocity.copy(
                    y = currentVelocity.y + (PhysicalLawEnforcement.GRAVITY / SUB_TICKS_PER_TICK)
                )
            }
            
            // Apply friction in sub-tick increments
            if (environment.isOnGround) {
                val frictionFactor = PhysicalLawEnforcement.GROUND_FRICTION.pow(1.0 / SUB_TICKS_PER_TICK.toDouble())
                currentVelocity = currentVelocity.copy(
                    x = currentVelocity.x * frictionFactor,
                    z = currentVelocity.z * frictionFactor
                )
            }
            
            // Apply air resistance in sub-tick increments
            val airResistanceFactor = PhysicalLawEnforcement.AIR_RESISTANCE.pow(1.0 / SUB_TICKS_PER_TICK.toDouble())
            currentVelocity = currentVelocity * airResistanceFactor
            
            // Update position
            currentPosition = currentPosition + currentVelocity
            path.add(currentPosition)
            
            // Check for collisions at each sub-tick
            if (environment.hasCollisions) {
                currentPosition = handleSubTickCollisions(currentPosition, environment)
                // Adjust velocity after collision
                if (path.size > 1) {
                    currentVelocity = currentPosition - path[path.size - 2]
                }
            }
        }
        
        return path
    }
    
    /**
     * Handle collisions at sub-tick level
     */
    private fun handleSubTickCollisions(position: Vector3D, environment: EnvironmentState): Vector3D {
        // Simplified collision handling - would need actual world data
        var correctedPosition = position
        
        // Basic ground collision
        if (environment.isOnGround && position.y < 0) {
            correctedPosition = correctedPosition.copy(y = 0.0)
        }
        
        // Basic ceiling collision (simplified)
        if (position.y > 256) {
            correctedPosition = correctedPosition.copy(y = 256.0)
        }
        
        return correctedPosition
    }
    
    /**
     * Calculate interpolation error between reconstructed and actual position
     */
    private fun calculateInterpolationError(reconstructedPath: List<Vector3D>, actualPosition: Vector3D): Double {
        if (reconstructedPath.isEmpty()) return Double.MAX_VALUE
        
        val reconstructedFinal = reconstructedPath.last()
        return reconstructedFinal.distanceTo(actualPosition)
    }
    
    /**
     * Perform advanced sub-tick validation checks
     */
    private fun performAdvancedSubTickChecks(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        reconstructedPath: List<Vector3D>,
        deltaTime: Long,
        environment: EnvironmentState
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        
        // Check for impossible sub-tick acceleration
        val actualAcceleration = calculateAverageAcceleration(from, to, deltaTime)
        val reconstructedAcceleration = calculatePathAcceleration(reconstructedPath, deltaTime)
        
        val accelerationDifference = abs(actualAcceleration - reconstructedAcceleration)
        if (accelerationDifference > 0.1) { // 0.1 blocks/tick² tolerance
            violations.add(createSubTickViolation(
                playerId, from, to, reconstructedPath.last(), accelerationDifference,
                "Impossible sub-tick acceleration: ${actualAcceleration} vs ${reconstructedAcceleration}"
            ))
        }
        
        // Check for sub-tick velocity anomalies
        val pathVelocityVariation = calculateVelocityVariation(reconstructedPath)
        if (pathVelocityVariation > 0.5 && !environment.isFlying) {
            violations.add(createSubTickViolation(
                playerId, from, to, reconstructedPath.last(), pathVelocityVariation,
                "Excessive velocity variation in sub-tick path"
            ))
        }
        
        // Check for temporal impossibilities
        val minTimeRequired = calculateMinimumTimeRequired(from, to, environment)
        if (deltaTime < minTimeRequired) {
            violations.add(createSubTickViolation(
                playerId, from, to, reconstructedPath.last(), 
                (minTimeRequired - deltaTime).toDouble(),
                "Movement completed faster than physically possible"
            ))
        }
        
        // Check for sub-tick pattern anomalies
        val history = playerSubTickHistory[playerId]
        if (history != null && history.size > 10) {
            val patternAnomaly = detectSubTickPatternAnomalies(history, reconstructedPath)
            if (patternAnomaly > 0.8) {
                violations.add(createSubTickViolation(
                    playerId, from, to, reconstructedPath.last(), patternAnomaly,
                    "Suspicious sub-tick movement pattern detected"
                ))
            }
        }
        
        return violations
    }
    
    /**
     * Calculate average acceleration between two points
     */
    private fun calculateAverageAcceleration(from: Vector3D, to: Vector3D, deltaTime: Long): Double {
        val distance = from.distanceTo(to)
        val timeSeconds = deltaTime / 1000.0
        return if (timeSeconds > 0) distance / (timeSeconds * timeSeconds) else 0.0
    }
    
    /**
     * Calculate acceleration from reconstructed path
     */
    private fun calculatePathAcceleration(path: List<Vector3D>, deltaTime: Long): Double {
        if (path.size < 3) return 0.0
        
        val velocities = mutableListOf<Double>()
        for (i in 1 until path.size) {
            val distance = path[i-1].distanceTo(path[i])
            val subTickTime = (deltaTime / path.size.toDouble()) / 1000.0
            velocities.add(distance / subTickTime)
        }
        
        if (velocities.size < 2) return 0.0
        
        val accelerations = mutableListOf<Double>()
        for (i in 1 until velocities.size) {
            val deltaV = velocities[i] - velocities[i-1]
            val subTickTime = (deltaTime / path.size.toDouble()) / 1000.0
            accelerations.add(deltaV / subTickTime)
        }
        
        return accelerations.average()
    }
    
    /**
     * Calculate velocity variation throughout the path
     */
    private fun calculateVelocityVariation(path: List<Vector3D>): Double {
        if (path.size < 3) return 0.0
        
        val velocities = mutableListOf<Double>()
        for (i in 1 until path.size) {
            velocities.add(path[i-1].distanceTo(path[i]))
        }
        
        val avgVelocity = velocities.average()
        val variance = velocities.map { (it - avgVelocity).pow(2.0) }.average()
        return sqrt(variance)
    }
    
    /**
     * Calculate minimum time required for movement
     */
    private fun calculateMinimumTimeRequired(from: Vector3D, to: Vector3D, environment: EnvironmentState): Long {
        val distance = from.distanceTo(to)
        val maxSpeed = when {
            environment.isFlying -> PhysicalLawEnforcement.MAX_FLY_SPEED * 20
            environment.isSprinting -> PhysicalLawEnforcement.MAX_SPRINT_SPEED * 20
            else -> PhysicalLawEnforcement.MAX_WALK_SPEED * 20
        }
        
        return ((distance / maxSpeed) * 1000).toLong()
    }
    
    /**
     * Detect pattern anomalies in sub-tick history
     */
    private fun detectSubTickPatternAnomalies(
        history: List<SubTickFrame>,
        currentPath: List<Vector3D>
    ): Double {
        // Simplified pattern detection
        val recentFrames = history.takeLast(10)
        val avgPathLength = recentFrames.map { it.path.size }.average()
        val avgInterpolationError = recentFrames.map { it.interpolationError }.average()
        
        val currentPathLength = currentPath.size
        val currentError = interpolationErrors.values.lastOrNull() ?: 0.0
        
        var anomalyScore = 0.0
        
        // Check for consistent path length deviations
        if (abs(currentPathLength - avgPathLength) > avgPathLength * 0.5) {
            anomalyScore += 0.3
        }
        
        // Check for consistent interpolation errors
        if (currentError > avgInterpolationError * 2) {
            anomalyScore += 0.4
        }
        
        // Check for too-perfect reconstruction (suspicious)
        if (currentError < RECONSTRUCTION_TOLERANCE * 0.1) {
            anomalyScore += 0.3
        }
        
        return anomalyScore.coerceAtMost(1.0)
    }
    
    /**
     * Update sub-tick movement history
     */
    private fun updateSubTickHistory(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        reconstructedPath: List<Vector3D>,
        deltaTime: Long
    ) {
        val history = playerSubTickHistory.computeIfAbsent(playerId) { mutableListOf() }
        
        val frame = SubTickFrame(
            from = from,
            to = to,
            path = reconstructedPath,
            deltaTime = deltaTime,
            interpolationError = interpolationErrors[playerId] ?: 0.0,
            timestamp = System.currentTimeMillis()
        )
        
        history.add(frame)
        
        // Keep only recent history
        if (history.size > 100) {
            history.removeAt(0)
        }
        
        lastTickPositions[playerId] = to
    }
    
    /**
     * Create sub-tick violation evidence
     */
    private fun createSubTickViolation(
        playerId: String,
        from: Vector3D,
        to: Vector3D,
        expected: Vector3D,
        error: Double,
        description: String
    ): Violation {
        return Violation(
            type = ViolationType.MOVEMENT_HACK,
            confidence = 0.92,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.TIMING_ANOMALY,
                    value = mapOf(
                        "from" to from,
                        "to" to to,
                        "expected" to expected,
                        "error" to error,
                        "description" to description,
                        "subTickAnalysis" to true
                    ),
                    confidence = 0.92,
                    description = "Sub-tick movement violation: $description"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Get player interpolation error statistics
     */
    fun getPlayerStats(playerId: String): Map<String, Any> {
        val history = playerSubTickHistory[playerId] ?: emptyList()
        val currentError = interpolationErrors[playerId] ?: 0.0
        
        return mapOf(
            "currentInterpolationError" to currentError,
            "averageError" to (history.map { it.interpolationError }.average().takeIf { !it.isNaN() } ?: 0.0),
            "maxError" to (history.map { it.interpolationError }.maxOrNull() ?: 0.0),
            "frameCount" to history.size,
            "isWithinTolerance" to (currentError <= RECONSTRUCTION_TOLERANCE)
        )
    }
    
    /**
     * Reset player sub-tick data
     */
    fun resetPlayerData(playerId: String, position: Vector3D) {
        playerSubTickHistory.remove(playerId)
        lastTickPositions[playerId] = position
        interpolationErrors.remove(playerId)
        
        logger.info { "🔄 Reset sub-tick data for player $playerId" }
    }
    
    /**
     * Cleanup player data
     */
    fun removePlayer(playerId: String) {
        playerSubTickHistory.remove(playerId)
        lastTickPositions.remove(playerId)
        interpolationErrors.remove(playerId)
    }
}

/**
 * Sub-tick frame data for tracking movement reconstruction
 */
data class SubTickFrame(
    val from: Vector3D,
    val to: Vector3D,
    val path: List<Vector3D>,
    val deltaTime: Long,
    val interpolationError: Double,
    val timestamp: Long
)

/**
 * Sub-tick validation result
 */
data class SubTickValidationResult(
    val isValid: Boolean,
    val violations: List<Violation>,
    val confidence: Double,
    val reconstructedPath: List<Vector3D>,
    val actualPosition: Vector3D,
    val interpolationError: Double,
    val numSubTicks: Int,
    val timestamp: Long
)