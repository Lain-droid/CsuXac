package com.csuxac.core.models

/**
 * Comprehensive player data for Ultimate Enforcement Directive v5.0 validation
 */
data class ComprehensivePlayerData(
    val playerId: String,
    val position: Vector3D,
    val velocity: Vector3D,
    val environment: ClientEnvironmentState,
    val action: PlayerAction,
    val behaviorData: BehaviorDataPoint,
    val packetData: PacketRecord,
    val timestamp: Long
)

/**
 * Comprehensive validation result combining all detection systems
 */
data class ComprehensiveValidationResult(
    override val isValid: Boolean,
    override val violations: List<Violation>,
    override val confidence: Double,
    val threatLevel: ThreatLevel,
    val realityValidation: RealityValidationResult?,
    val causalValidation: CausalValidationResult?,
    val behaviorValidation: BehaviorValidationResult?,
    val temporalValidation: TemporalPacketValidationResult?,
    override val timestamp: Long
) : ValidationResult(isValid, violations, confidence, timestamp)

/**
 * Threat level classification
 */
enum class ThreatLevel(val severity: Int, val description: String) {
    SAFE(0, "No threats detected"),
    LOW(1, "Minor suspicious activity"),
    MEDIUM(5, "Moderate threat level"),
    HIGH(15, "High threat level"),
    CRITICAL(25, "Critical threat - immediate action required")
}

/**
 * Client environment state for reality fork detection
 */
data class ClientEnvironmentState(
    val isOnGround: Boolean,
    val isFlying: Boolean,
    val isSprinting: Boolean,
    val isInFluid: Boolean,
    val blockType: String? = null,
    val fluidLevel: Float = 0f
)