package com.csuxac.core.models

/**
 * Base class for all validation results
 */
sealed class ValidationResult(
    open val isValid: Boolean,
    open val violations: List<Violation>,
    open val confidence: Double,
    open val timestamp: Long
)

/**
 * Result of movement validation
 */
data class MovementValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    override val timestamp: Long,
    val movementType: MovementType? = null,
    val distance: Double? = null,
    val speed: Double? = null,
    val physicsCompliant: Boolean = true
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Result of action validation
 */
data class ActionValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    override val timestamp: Long,
    val actionType: ActionType? = null,
    val riskAssessment: RiskAssessment? = null,
    val behaviorScore: Double = 0.0
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Result of velocity validation
 */
data class VelocityValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    override val timestamp: Long,
    val expectedVelocity: Vector3D? = null,
    val actualVelocity: Vector3D? = null,
    val velocityDifference: Vector3D? = null,
    val consistencyScore: Double = 0.0
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Result of packet validation
 */
data class PacketValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    override val timestamp: Long,
    val packetType: String? = null,
    val flowPattern: PacketFlowPattern? = null,
    val timingAnomaly: Boolean = false,
    val fingerprintMatch: Double = 0.0
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Result of physics simulation
 */
data class PhysicsValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    override val timestamp: Long,
    val simulatedPosition: Vector3D? = null,
    val actualPosition: Vector3D? = null,
    val collisionResult: CollisionResult? = null,
    val physicsScore: Double = 0.0
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Result of behavior pattern analysis
 */
data class BehaviorValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    override val timestamp: Long,
    val entropyScore: Double = 0.0,
    val patternType: BehaviorPattern? = null,
    val humanLikeness: Double = 0.0,
    val anomalyScore: Double = 0.0
) : ValidationResult(isValid, violations, confidence, timestamp)

// Supporting enums and data classes
enum class MovementType {
    WALK, SPRINT, JUMP, FLY, PHASE, TELEPORT, FALL
}

enum class RiskAssessment {
    SAFE, LOW_RISK, MEDIUM_RISK, HIGH_RISK, CRITICAL
}

enum class PacketFlowPattern {
    NORMAL, COMPRESSED, DELAYED, SPOOFED, ANOMALOUS
}

enum class CollisionResult {
    NO_COLLISION, BLOCK_COLLISION, ENTITY_COLLISION, BOUNDARY_COLLISION
}

enum class BehaviorPattern {
    HUMAN, BOT, MACRO, SCRIPT, UNKNOWN
}

/**
 * Player security session for tracking behavior over time
 */
data class PlayerSecuritySession(
    val playerId: String,
    val startTime: Long,
    var lastActivity: Long = startTime,
    var totalActions: Int = 0,
    var violationHistory: MutableList<Violation> = mutableListOf(),
    var behaviorMetrics: BehaviorMetrics = BehaviorMetrics(),
    var packetFingerprint: PacketFingerprint = PacketFingerprint(),
    var physicsProfile: PhysicsProfile = PhysicsProfile()
) {
    fun updateActivity() {
        lastActivity = System.currentTimeMillis()
        totalActions++
    }
    
    fun addViolation(violation: Violation) {
        violationHistory.add(violation)
        if (violationHistory.size > 100) {
            violationHistory.removeAt(0) // Keep only last 100 violations
        }
    }
    
    fun getRecentViolations(withinMs: Long): List<Violation> {
        val cutoff = System.currentTimeMillis() - withinMs
        return violationHistory.filter { it.timestamp > cutoff }
    }
    
    fun getViolationCount(type: ViolationType): Int {
        return violationHistory.count { it.type == type }
    }
}

data class BehaviorMetrics(
    var clickEntropy: Double = 0.0,
    var movementEntropy: Double = 0.0,
    var actionPatterns: MutableMap<String, Int> = mutableMapOf(),
    var timingVariance: Double = 0.0,
    var lastUpdate: Long = System.currentTimeMillis()
)

data class PacketFingerprint(
    var packetFlowPattern: PacketFlowPattern = PacketFlowPattern.NORMAL,
    var averagePacketSize: Double = 0.0,
    var packetTimingVariance: Double = 0.0,
    var compressionRatio: Double = 1.0,
    var lastUpdate: Long = System.currentTimeMillis()
)

data class PhysicsProfile(
    var averageSpeed: Double = 0.0,
    var maxSpeed: Double = 0.0,
    var accelerationPattern: Double = 0.0,
    var collisionFrequency: Double = 0.0,
    var lastUpdate: Long = System.currentTimeMillis()
)