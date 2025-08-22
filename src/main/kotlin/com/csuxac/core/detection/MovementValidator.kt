package com.csuxac.core.detection

import com.csuxac.config.MovementConfig
import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * MovementValidator - Comprehensive movement validation system
 * 
 * Detects:
 * - Fly hacks (AAC, NCP, Verus bypass)
 * - Phase hacks (collision box penetration)
 * - Speed hacks (movement speed violations)
 * - Timer hacks (sub-tick movement anomalies)
 * - Scaffold hacks (block placement timing)
 * - NoFall hacks (fall damage bypass)
 */
class MovementValidator(
    private val config: MovementConfig
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Movement history tracking for pattern analysis
    private val movementHistory = mutableMapOf<String, MutableList<MovementRecord>>()
    private val lastPositions = mutableMapOf<String, Vector3D>()
    private val lastVelocities = mutableMapOf<String, Vector3D>()
    private val lastTimestamps = mutableMapOf<String, Long>()
    
    // Constants for Minecraft physics
    companion object {
        const val MAX_WALK_SPEED = 0.2158 // Maximum walking speed
        const val MAX_SPRINT_SPEED = 0.2806 // Maximum sprinting speed
        const val MAX_FLY_SPEED = 0.4 // Maximum flying speed
        const val GRAVITY = -0.08 // Gravity constant
        const val AIR_RESISTANCE = 0.98 // Air resistance
        const val GROUND_FRICTION = 0.6 // Ground friction
        const val JUMP_VELOCITY = 0.42 // Jump velocity
        const val STEP_HEIGHT = 0.6 // Maximum step height
        const val PLAYER_HEIGHT = 1.8 // Player height
        const val PLAYER_WIDTH = 0.6 // Player width
    }
    
    /**
     * Validate player movement with zero tolerance for violations
     */
    suspend fun validateMovement(
        from: Vector3D,
        to: Vector3D,
        timestamp: Long
    ): MovementValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Calculate movement parameters
            val distance = from.distanceTo(to)
            val timeDelta = timestamp - (lastTimestamps[from.toString()] ?: timestamp)
            val speed = if (timeDelta > 0) distance / (timeDelta / 1000.0) else 0.0
            
            // 1. Basic speed validation
            val speedViolation = validateSpeed(speed, from, to)
            if (speedViolation != null) {
                violations.add(speedViolation)
                confidence *= 0.7
            }
            
            // 2. Fly detection
            val flyViolation = detectFlyHack(from, to, timestamp)
            if (flyViolation != null) {
                violations.add(flyViolation)
                confidence *= 0.5
            }
            
            // 3. Phase detection
            val phaseViolation = detectPhaseHack(from, to)
            if (phaseViolation != null) {
                violations.add(phaseViolation)
                confidence *= 0.3
            }
            
            // 4. Timer hack detection
            val timerViolation = detectTimerHack(from, to, timestamp)
            if (timerViolation != null) {
                violations.add(timerViolation)
                confidence *= 0.6
            }
            
            // 5. Scaffold detection
            val scaffoldViolation = detectScaffoldHack(from, to, timestamp)
            if (scaffoldViolation != null) {
                violations.add(scaffoldViolation)
                confidence *= 0.8
            }
            
            // 6. NoFall detection
            val noFallViolation = detectNoFallHack(from, to, timestamp)
            if (noFallViolation != null) {
                violations.add(noFallViolation)
                confidence *= 0.7
            }
            
            // 7. Sub-tick anomaly detection
            val subTickViolation = detectSubTickAnomaly(from, to, timestamp)
            if (subTickViolation != null) {
                violations.add(subTickViolation)
                confidence *= 0.4
            }
            
            // Update movement history
            updateMovementHistory(from, to, timestamp, speed)
            
            // Determine movement type
            val movementType = determineMovementType(from, to, speed)
            
            // Calculate final confidence
            confidence = max(0.1, confidence)
            
            // Update tracking
            lastTimestamps[to.toString()] = timestamp
            
            return MovementValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                timestamp = timestamp,
                movementType = movementType,
                distance = distance,
                speed = speed,
                physicsCompliant = violations.none { it.type == ViolationType.PHASE_HACK }
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error during movement validation" }
            violations.add(
                Violation(
                    type = ViolationType.MOVEMENT_HACK,
                    confidence = 0.9,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.PHYSICS_VIOLATION,
                            value = "Validation error: ${e.message}",
                            confidence = 0.9,
                            description = "Movement validation failed due to system error"
                        )
                    ),
                    timestamp = timestamp,
                    playerId = "UNKNOWN"
                )
            )
            
            return MovementValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.1,
                timestamp = timestamp
            )
        }
    }
    
    private fun validateSpeed(speed: Double, from: Vector3D, to: Vector3D): Violation? {
        val maxAllowedSpeed = when {
            isOnGround(from) -> MAX_SPRINT_SPEED
            isFlying(from) -> MAX_FLY_SPEED
            else -> MAX_WALK_SPEED
        }
        
        if (speed > maxAllowedSpeed * 1.1) { // 10% tolerance
            return Violation(
                type = ViolationType.SPEED_HACK,
                confidence = 0.95,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PHYSICS_VIOLATION,
                        value = speed,
                        confidence = 0.95,
                        description = "Movement speed $speed exceeds maximum allowed $maxAllowedSpeed"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = "UNKNOWN"
            )
        }
        
        return null
    }
    
    private fun detectFlyHack(from: Vector3D, to: Vector3D, timestamp: Long): Violation? {
        // Check if player is moving upward without proper physics
        val yDelta = to.y - from.y
        
        if (yDelta > 0 && !isOnGround(from) && !isFlying(from)) {
            // Check if this is a valid jump
            val lastVelocity = lastVelocities[from.toString()]
            val expectedY = from.y + (lastVelocity?.y ?: 0.0) + GRAVITY
            
            if (abs(to.y - expectedY) > 0.1) {
                return Violation(
                    type = ViolationType.FLY_HACK,
                    confidence = 0.9,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.PHYSICS_VIOLATION,
                            value = "Y delta: $yDelta, Expected: ${expectedY - from.y}",
                            confidence = 0.9,
                            description = "Invalid upward movement detected"
                        )
                    ),
                    timestamp = timestamp,
                    playerId = "UNKNOWN"
                )
            }
        }
        
        return null
    }
    
    private fun detectPhaseHack(from: Vector3D, to: Vector3D): Violation? {
        // Check if player is moving through solid blocks
        val path = calculatePath(from, to)
        
        for (block in path) {
            if (isSolidBlock(block)) {
                return Violation(
                    type = ViolationType.PHASE_HACK,
                    confidence = 0.95,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.PHYSICS_VIOLATION,
                            value = "Block at $block",
                            confidence = 0.95,
                            description = "Player moved through solid block"
                        )
                    ),
                    timestamp = System.currentTimeMillis(),
                    playerId = "UNKNOWN"
                )
            }
        }
        
        return null
    }
    
    private fun detectTimerHack(from: Vector3D, to: Vector3D, timestamp: Long): Violation? {
        // Check for sub-tick movement anomalies
        val lastRecord = movementHistory[from.toString()]?.lastOrNull()
        
        if (lastRecord != null) {
            val timeDelta = timestamp - lastRecord.timestamp
            val expectedTimeDelta = 50 // 20 TPS = 50ms per tick
            
            if (timeDelta < expectedTimeDelta * 0.8) { // 20% tolerance
                return Violation(
                    type = ViolationType.TIMER_HACK,
                    confidence = 0.85,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.TIMING_ANOMALY,
                            value = "Time delta: $timeDelta, Expected: $expectedTimeDelta",
                            confidence = 0.85,
                            description = "Sub-tick movement detected"
                        )
                    ),
                    timestamp = timestamp,
                    playerId = "UNKNOWN"
                )
            }
        }
        
        return null
    }
    
    private fun detectScaffoldHack(from: Vector3D, to: Vector3D, timestamp: Long): Violation? {
        // Check for scaffold-like movement patterns
        val yDelta = to.y - from.y
        val horizontalDistance = sqrt((to.x - from.x).pow(2) + (to.z - from.z).pow(2))
        
        // Scaffold typically involves moving horizontally while falling
        if (yDelta < -0.1 && horizontalDistance > 0.5) {
            val lastRecord = movementHistory[from.toString()]?.lastOrNull()
            
            if (lastRecord != null && lastRecord.yDelta < -0.1) {
                return Violation(
                    type = ViolationType.SCAFFOLD_HACK,
                    confidence = 0.8,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.PATTERN_DETECTION,
                            value = "Y delta: $yDelta, Horizontal: $horizontalDistance",
                            confidence = 0.8,
                            description = "Scaffold movement pattern detected"
                        )
                    ),
                    timestamp = timestamp,
                    playerId = "UNKNOWN"
                )
            }
        }
        
        return null
    }
    
    private fun detectNoFallHack(from: Vector3D, to: Vector3D, timestamp: Long): Violation? {
        // Check if player should take fall damage
        val fallDistance = calculateFallDistance(from, to)
        
        if (fallDistance > 4.0 && !isOnGround(to)) {
            return Violation(
                type = ViolationType.MOVEMENT_HACK,
                confidence = 0.75,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PHYSICS_VIOLATION,
                        value = "Fall distance: $fallDistance",
                        confidence = 0.75,
                        description = "Player should take fall damage"
                    )
                ),
                timestamp = timestamp,
                playerId = "UNKNOWN"
            )
        }
        
        return null
    }
    
    private fun detectSubTickAnomaly(from: Vector3D, to: Vector3D, timestamp: Long): Violation? {
        // Check for movement that happens between server ticks
        val lastRecord = movementHistory[from.toString()]?.lastOrNull()
        
        if (lastRecord != null) {
            val timeDelta = timestamp - lastRecord.timestamp
            val distance = from.distanceTo(to)
            val speed = distance / (timeDelta / 1000.0)
            
            // If speed is impossibly high for the time delta
            if (speed > MAX_SPRINT_SPEED * 2 && timeDelta < 25) {
                return Violation(
                    type = ViolationType.TIMER_HACK,
                    confidence = 0.9,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.TIMING_ANOMALY,
                            value = "Speed: $speed, Time: $timeDelta",
                            confidence = 0.9,
                            description = "Impossible speed for time delta"
                        )
                    ),
                    timestamp = timestamp,
                    playerId = "UNKNOWN"
                )
            }
        }
        
        return null
    }
    
    private fun updateMovementHistory(from: Vector3D, to: Vector3D, timestamp: Long, speed: Double) {
        val key = from.toString()
        val record = MovementRecord(from, to, timestamp, speed, to.y - from.y)
        
        movementHistory.getOrPut(key) { mutableListOf() }.add(record)
        
        // Keep only last 100 records
        if (movementHistory[key]!!.size > 100) {
            movementHistory[key]!!.removeAt(0)
        }
        
        lastPositions[key] = to
    }
    
    private fun determineMovementType(from: Vector3D, to: Vector3D, speed: Double): MovementType {
        val yDelta = to.y - from.y
        val horizontalDistance = sqrt((to.x - from.x).pow(2) + (to.z - from.z).pow(2))
        
        return when {
            yDelta > 0.1 -> MovementType.JUMP
            yDelta < -0.1 -> MovementType.FALL
            horizontalDistance > 0.5 -> {
                when {
                    speed > MAX_SPRINT_SPEED -> MovementType.SPRINT
                    else -> MovementType.WALK
                }
            }
            else -> MovementType.WALK
        }
    }
    
    // Helper functions (simplified for demonstration)
    private fun isOnGround(position: Vector3D): Boolean = position.y % 1.0 < 0.1
    private fun isFlying(position: Vector3D): Boolean = false // Would check player permissions
    private fun isSolidBlock(position: Vector3D): Boolean = false // Would check world data
    private fun calculatePath(from: Vector3D, to: Vector3D): List<Vector3D> = listOf(from, to)
    private fun calculateFallDistance(from: Vector3D, to: Vector3D): Double = max(0.0, from.y - to.y)
    
    private data class MovementRecord(
        val from: Vector3D,
        val to: Vector3D,
        val timestamp: Long,
        val speed: Double,
        val yDelta: Double
    )
}