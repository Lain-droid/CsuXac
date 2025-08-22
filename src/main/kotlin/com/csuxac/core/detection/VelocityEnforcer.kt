package com.csuxac.core.detection

import com.csuxac.config.VelocityConfig
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

/**
 * VelocityEnforcer - Velocity consistency and knockback validation system
 * 
 * Features:
 * - Velocity consistency enforcement
 * - Knockback validation
 * - Desync detection
 * - Gravity enforcement
 * - Immediate rollback on violations
 */
class VelocityEnforcer(
    private val config: VelocityConfig
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Velocity tracking per player
    private val velocityHistory = mutableMapOf<String, MutableList<VelocityRecord>>()
    private val lastVelocities = mutableMapOf<String, Vector3D>()
    private val expectedVelocities = mutableMapOf<String, Vector3D>()
    
    /**
     * Validate velocity consistency
     */
    suspend fun validateVelocity(
        expectedVelocity: Vector3D,
        actualVelocity: Vector3D,
        session: PlayerSecuritySession
    ): VelocityValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // 1. Basic velocity validation
            val basicViolation = validateBasicVelocity(expectedVelocity, actualVelocity)
            if (basicViolation != null) {
                violations.add(basicViolation)
                confidence *= 0.7
            }
            
            // 2. Consistency check
            val consistencyViolation = validateVelocityConsistency(session.playerId, expectedVelocity, actualVelocity)
            if (consistencyViolation != null) {
                violations.add(consistencyViolation)
                confidence *= 0.6
            }
            
            // 3. Knockback validation
            val knockbackViolation = validateKnockback(session.playerId, expectedVelocity, actualVelocity)
            if (knockbackViolation != null) {
                violations.add(knockbackViolation)
                confidence *= 0.8
            }
            
            // 4. Desync detection
            val desyncViolation = detectDesync(session.playerId, expectedVelocity, actualVelocity)
            if (desyncViolation != null) {
                violations.add(desyncViolation)
                confidence *= 0.5
            }
            
            // 5. Gravity enforcement
            val gravityViolation = validateGravity(session.playerId, expectedVelocity, actualVelocity)
            if (gravityViolation != null) {
                violations.add(gravityViolation)
                confidence *= 0.9
            }
            
            // Update velocity history
            updateVelocityHistory(session.playerId, expectedVelocity, actualVelocity)
            
            // Calculate consistency score
            val consistencyScore = calculateConsistencyScore(session.playerId)
            
            // Calculate velocity difference
            val velocityDifference = actualVelocity - expectedVelocity
            
            return VelocityValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                timestamp = System.currentTimeMillis(),
                expectedVelocity = expectedVelocity,
                actualVelocity = actualVelocity,
                velocityDifference = velocityDifference,
                consistencyScore = consistencyScore
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error during velocity validation for player ${session.playerId}" }
            violations.add(
                Violation(
                    type = ViolationType.VELOCITY_HACK,
                    confidence = 0.9,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.VELOCITY_ANOMALY,
                            value = "Validation error: ${e.message}",
                            confidence = 0.9,
                            description = "Velocity validation failed due to system error"
                        )
                    ),
                    timestamp = System.currentTimeMillis(),
                    playerId = session.playerId
                )
            )
            
            return VelocityValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.1,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Validate basic velocity parameters
     */
    private fun validateBasicVelocity(expected: Vector3D, actual: Vector3D): Violation? {
        // Check if velocity magnitude is within reasonable bounds
        val expectedMagnitude = expected.magnitude()
        val actualMagnitude = actual.magnitude()
        
        if (actualMagnitude > config.maxVelocity) {
            return Violation(
                type = ViolationType.VELOCITY_HACK,
                confidence = 0.95,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.VELOCITY_ANOMALY,
                        value = "Velocity magnitude: $actualMagnitude, Max allowed: ${config.maxVelocity}",
                        confidence = 0.95,
                        description = "Velocity magnitude exceeds maximum allowed value"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = "UNKNOWN"
            )
        }
        
        // Check if velocity difference is too large
        val difference = (actual - expected).magnitude()
        val tolerance = config.tolerance
        
        if (difference > tolerance) {
            return Violation(
                type = ViolationType.VELOCITY_HACK,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.VELOCITY_ANOMALY,
                        value = "Velocity difference: $difference, Tolerance: $tolerance",
                        confidence = 0.9,
                        description = "Velocity difference exceeds tolerance"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = "UNKNOWN"
            )
        }
        
        return null
    }
    
    /**
     * Validate velocity consistency over time
     */
    private fun validateVelocityConsistency(
        playerId: String,
        expected: Vector3D,
        actual: Vector3D
    ): Violation? {
        if (!config.consistencyCheck) return null
        
        val history = velocityHistory[playerId] ?: return null
        if (history.size < 3) return null
        
        // Check for sudden velocity changes
        val recentVelocities = history.takeLast(5)
        val avgVelocity = recentVelocities.map { it.actualVelocity }.let { velocities ->
            Vector3D(
                velocities.map { it.x }.average(),
                velocities.map { it.y }.average(),
                velocities.map { it.z }.average()
            )
        }
        
        val currentDifference = (actual - avgVelocity).magnitude()
        val avgMagnitude = avgVelocity.magnitude()
        
        // If current velocity deviates too much from recent average
        if (avgMagnitude > 0.1 && currentDifference > avgMagnitude * 0.5) {
            return Violation(
                type = ViolationType.VELOCITY_HACK,
                confidence = 0.85,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.VELOCITY_ANOMALY,
                        value = "Velocity deviation: $currentDifference, Average: $avgMagnitude",
                        confidence = 0.85,
                        description = "Sudden velocity change detected"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Validate knockback consistency
     */
    private fun validateKnockback(
        playerId: String,
        expected: Vector3D,
        actual: Vector3D
    ): Violation? {
        if (!config.knockbackValidation) return null
        
        val history = velocityHistory[playerId] ?: return null
        if (history.size < 2) return null
        
        val lastRecord = history.last()
        val lastVelocity = lastRecord.actualVelocity
        
        // Check if knockback is applied correctly
        val knockbackDifference = (actual - lastVelocity).magnitude()
        val expectedKnockback = (expected - lastVelocity).magnitude()
        
        // If actual knockback differs significantly from expected
        if (abs(knockbackDifference - expectedKnockback) > 0.2) {
            return Violation(
                type = ViolationType.VELOCITY_HACK,
                confidence = 0.8,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.VELOCITY_ANOMALY,
                        value = "Actual knockback: $knockbackDifference, Expected: $expectedKnockback",
                        confidence = 0.8,
                        description = "Knockback validation failed"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Detect velocity desync
     */
    private fun detectDesync(
        playerId: String,
        expected: Vector3D,
        actual: Vector3D
    ): Violation? {
        if (!config.desyncDetection) return null
        
        val history = velocityHistory[playerId] ?: return null
        if (history.size < 5) return null
        
        // Check for systematic velocity differences
        val recentRecords = history.takeLast(10)
        val systematicDifferences = recentRecords.count { record ->
            val diff = (record.actualVelocity - record.expectedVelocity).magnitude()
            diff > config.tolerance
        }
        
        // If more than 70% of recent velocities show differences
        if (systematicDifferences > recentRecords.size * 0.7) {
            return Violation(
                type = ViolationType.VELOCITY_HACK,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.VELOCITY_ANOMALY,
                        value = "Systematic differences: $systematicDifferences/${recentRecords.size}",
                        confidence = 0.9,
                        description = "Velocity desync detected"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Validate gravity enforcement
     */
    private fun validateGravity(
        playerId: String,
        expected: Vector3D,
        actual: Vector3D
    ): Violation? {
        if (!config.gravityEnforcement) return null
        
        val history = velocityHistory[playerId] ?: return null
        if (history.size < 2) return null
        
        val lastRecord = history.last()
        val lastVelocity = lastRecord.actualVelocity
        
        // Check if Y velocity follows gravity rules
        val yDelta = actual.y - lastVelocity.y
        val expectedYDelta = -0.08 // Gravity constant
        
        // Allow some tolerance for gravity
        if (abs(yDelta - expectedYDelta) > 0.05) {
            return Violation(
                type = ViolationType.VELOCITY_HACK,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PHYSICS_VIOLATION,
                        value = "Y velocity change: $yDelta, Expected: $expectedYDelta",
                        confidence = 0.9,
                        description = "Gravity violation detected"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Update velocity history
     */
    private fun updateVelocityHistory(
        playerId: String,
        expected: Vector3D,
        actual: Vector3D
    ) {
        val history = velocityHistory.getOrPut(playerId) { mutableListOf() }
        
        val record = VelocityRecord(
            timestamp = System.currentTimeMillis(),
            expectedVelocity = expected,
            actualVelocity = actual,
            difference = (actual - expected).magnitude()
        )
        
        history.add(record)
        
        // Keep only recent history
        if (history.size > 100) {
            history.removeAt(0)
        }
        
        // Update last velocities
        lastVelocities[playerId] = actual
        expectedVelocities[playerId] = expected
    }
    
    /**
     * Calculate velocity consistency score
     */
    private fun calculateConsistencyScore(playerId: String): Double {
        val history = velocityHistory[playerId] ?: return 1.0
        if (history.size < 5) return 1.0
        
        val recentRecords = history.takeLast(10)
        val differences = recentRecords.map { it.difference }
        val avgDifference = differences.average()
        
        // Lower difference = higher consistency
        val consistencyScore = max(0.0, 1.0 - avgDifference)
        
        return consistencyScore
    }
    
    /**
     * Get velocity statistics for player
     */
    fun getVelocityStats(playerId: String): VelocityStats? {
        val history = velocityHistory[playerId] ?: return null
        
        return VelocityStats(
            playerId = playerId,
            totalRecords = history.size,
            averageDifference = history.map { it.difference }.average(),
            maxDifference = history.map { it.difference }.maxOrNull() ?: 0.0,
            consistencyScore = calculateConsistencyScore(playerId),
            lastUpdate = history.lastOrNull()?.timestamp ?: 0
        )
    }
    
    /**
     * Check if player should be frozen due to desync
     */
    fun shouldFreezePlayer(playerId: String): Boolean {
        if (!config.freezeOnDesync) return false
        
        val history = velocityHistory[playerId] ?: return false
        if (history.size < 10) return false
        
        val recentRecords = history.takeLast(10)
        val desyncCount = recentRecords.count { record ->
            record.difference > config.tolerance * 2
        }
        
        // Freeze if more than 80% of recent velocities show severe desync
        return desyncCount > recentRecords.size * 0.8
    }
    
    /**
     * Reset velocity tracking for player
     */
    fun resetPlayerVelocity(playerId: String) {
        velocityHistory.remove(playerId)
        lastVelocities.remove(playerId)
        expectedVelocities.remove(playerId)
        
        logger.info { "🔄 Reset velocity tracking for player $playerId" }
    }
    
    // Data classes
    private data class VelocityRecord(
        val timestamp: Long,
        val expectedVelocity: Vector3D,
        val actualVelocity: Vector3D,
        val difference: Double
    )
    
    data class VelocityStats(
        val playerId: String,
        val totalRecords: Int,
        val averageDifference: Double,
        val maxDifference: Double,
        val consistencyScore: Double,
        val lastUpdate: Long
    )
}