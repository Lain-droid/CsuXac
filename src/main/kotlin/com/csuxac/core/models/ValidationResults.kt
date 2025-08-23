package com.csuxac.core.models

// EnvironmentState is defined locally to avoid circular dependencies

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

/**
 * Reality validation result
 */
data class RealityValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    val clientReality: ClientReality,
    val serverReality: ServerReality,
    val divergence: Double,
    override val timestamp: Long
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Causal validation result
 */
data class CausalValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    val action: PlayerAction,
    val causalChain: List<PlayerAction>,
    val validationDetails: CausalActionValidation,
    override val timestamp: Long
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Temporal packet validation result
 */
data class TemporalPacketValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    val temporalPacket: TemporalPacketRecord?,
    val temporalAnalysis: TemporalAnomalyAnalysis?,
    val entropyAnalysis: EntropyAnalysis?,
    override val timestamp: Long
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Causal action validation
 */
data class CausalActionValidation(
    val isValid: Boolean,
    val violations: List<Violation>,
    val reason: String
)

/**
 * Client reality state for validation
 */
data class ClientReality(
    val position: Vector3D,
    val velocity: Vector3D,
    val environment: ClientEnvironmentState
)

/**
 * Server reality state for validation
 */
data class ServerReality(
    val position: Vector3D,
    val velocity: Vector3D,
    val environment: EnvironmentState
)

/**
 * Temporal packet record with 4D coordinates
 */
data class TemporalPacketRecord(
    val packet: PacketRecord,
    val timestamp: Long,
    val spatialPosition: Vector3D,
    val temporalVelocity: Double
)

/**
 * Temporal anomaly analysis result
 */
data class TemporalAnomalyAnalysis(
    val anomalyScore: Double,
    val anomalyType: String,
    val componentScores: Map<String, Double>,
    val performanceScore: Double
)

/**
 * Entropy analysis result
 */
data class EntropyAnalysis(
    val timingEntropy: Double,
    val spatialEntropy: Double,
    val typeEntropy: Double
)

/**
 * Reality divergence statistics
 */
data class RealityDivergenceStats(
    val playerId: String,
    val totalDivergences: Int,
    val lastSyncTime: Long,
    val isQuarantined: Boolean
)

/**
 * Causal chain statistics
 */
data class CausalChainStats(
    val playerId: String,
    val totalActions: Int,
    val totalRelationships: Int,
    val causalViolations: Int,
    val lastActionTime: Long,
    val isQuarantined: Boolean
)

/**
 * Behavior data point for analysis
 */
data class BehaviorDataPoint(
    val playerId: String,
    val dataType: BehaviorDataType,
    val timestamp: Long,
    val movementData: MovementBehaviorData? = null,
    val mouseData: MouseBehaviorData? = null,
    val actionData: ActionBehaviorData? = null
)

/**
 * Movement behavior data
 */
data class MovementBehaviorData(
    val movementVector: Vector3D,
    val speed: Double,
    val acceleration: Double
)

/**
 * Mouse behavior data
 */
data class MouseBehaviorData(
    val movementDelta: Vector3D,
    val clickPosition: Vector3D?,
    val scrollDelta: Double
)

/**
 * Action behavior data
 */
data class ActionBehaviorData(
    val actionType: ActionType,
    val targetPosition: Vector3D?,
    val actionDelay: Long
)

/**
 * Behavior data types
 */
enum class BehaviorDataType {
    MOVEMENT, MOUSE, ACTION
}

/**
 * Behavior profile for a player
 */
data class BehaviorProfile(
    val playerId: String,
    var movementEntropy: Double,
    var mouseEntropy: Double,
    var timingVariance: Double,
    val actionFrequencyDistribution: MutableMap<ActionType, Double>,
    var totalSamples: Int,
    val createdAt: Long,
    var lastUpdate: Long
)

/**
 * Behavior anomaly analysis result
 */
data class BehaviorAnomalyAnalysis(
    val anomalyScore: Double,
    val anomalyType: String,
    val componentScores: Map<String, Double>
)

/**
 * Behavior analysis statistics
 */
data class BehaviorAnalysisStats(
    val playerId: String,
    val totalSamples: Int,
    val movementEntropy: Double,
    val mouseEntropy: Double,
    val timingVariance: Double,
    val anomalyCount: Int,
    val isQuarantined: Boolean
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