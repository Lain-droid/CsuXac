package com.csuxac.config

/**
 * Main security configuration for CsuXac Core
 */
data class SecurityConfig(
    val movement: MovementConfig = MovementConfig(),
    val packet: PacketConfig = PacketConfig(),
    val physics: PhysicsConfig = PhysicsConfig(),
    val behavior: BehaviorConfig = BehaviorConfig(),
    val velocity: VelocityConfig = VelocityConfig(),
    val challenge: ChallengeConfig = ChallengeConfig(),
    val exploits: ExploitConfig = ExploitConfig(),
    val adaptation: AdaptationConfig = AdaptationConfig(),
    val enforcement: EnforcementConfig = EnforcementConfig(),
    val quarantine: QuarantineConfig = QuarantineConfig(),
    val rollback: RollbackConfig = RollbackConfig(),
    val intelligence: IntelligenceConfig = IntelligenceConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    val anomaly: AnomalyConfig = AnomalyConfig()
) {
    companion object {
        fun load(): SecurityConfig {
            return SecurityConfig()
        }
    }
}

/**
 * Movement validation configuration
 */
data class MovementConfig(
    val enabled: Boolean = true,
    val maxWalkSpeed: Double = 0.2158,
    val maxSprintSpeed: Double = 0.2806,
    val maxFlySpeed: Double = 0.4,
    val gravity: Double = -0.08,
    val airResistance: Double = 0.98,
    val groundFriction: Double = 0.6,
    val jumpVelocity: Double = 0.42,
    val stepHeight: Double = 0.6,
    val playerHeight: Double = 1.8,
    val playerWidth: Double = 0.6,
    val tolerance: Double = 0.1,
    val historySize: Int = 100,
    val detectionThreshold: Double = 0.8
)

/**
 * Packet analysis configuration
 */
data class PacketConfig(
    val enabled: Boolean = true,
    val maxPacketHistory: Int = 1000,
    val analysisWindow: Long = 1000, // 1 second
    val timingTolerance: Long = 5, // 5ms
    val compressionThreshold: Double = 0.8,
    val fingerprintThreshold: Double = 0.7,
    val anomalyThreshold: Double = 0.7,
    val subTickThreshold: Long = 45, // 45ms
    val patternMatching: Boolean = true,
    val clientFingerprinting: Boolean = true
)

/**
 * Physics simulation configuration
 */
data class PhysicsConfig(
    val enabled: Boolean = true,
    val simulationAccuracy: Double = 0.99,
    val collisionDetection: Boolean = true,
    val gravitySimulation: Boolean = true,
    val fluidDynamics: Boolean = true,
    val blockResistance: Boolean = true,
    val stepHeight: Boolean = true,
    val airDrag: Boolean = true,
    val maxSimulationSteps: Int = 100,
    val physicsTickRate: Int = 20
)

/**
 * Behavior analysis configuration
 */
data class BehaviorConfig(
    val enabled: Boolean = true,
    val entropyAnalysis: Boolean = true,
    val patternRecognition: Boolean = true,
    val humanLikeness: Boolean = true,
    val clickAnalysis: Boolean = true,
    val movementAnalysis: Boolean = true,
    val actionAnalysis: Boolean = true,
    val historySize: Int = 1000,
    val entropyThreshold: Double = 0.6,
    val patternThreshold: Double = 0.7,
    val humanThreshold: Double = 0.8
)

/**
 * Velocity enforcement configuration
 */
data class VelocityConfig(
    val enabled: Boolean = true,
    val consistencyCheck: Boolean = true,
    val rollbackOnViolation: Boolean = true,
    val tolerance: Double = 0.1,
    val maxVelocity: Double = 2.0,
    val gravityEnforcement: Boolean = true,
    val knockbackValidation: Boolean = true,
    val desyncDetection: Boolean = true,
    val freezeOnDesync: Boolean = true
)

/**
 * Challenge-response configuration
 */
data class ChallengeConfig(
    val enabled: Boolean = true,
    val challengeFrequency: Long = 30000, // 30 seconds
    val maxChallenges: Int = 5,
    val responseTimeout: Long = 5000, // 5 seconds
    val challengeTypes: List<String> = listOf(
        "position_verification",
        "block_reach_test",
        "movement_validation",
        "physics_simulation",
        "timing_verification"
    ),
    val quarantineOnFailure: Boolean = true
)

/**
 * Exploit-specific detection configuration
 */
data class ExploitConfig(
    val enabled: Boolean = true,
    val flyDetection: Boolean = true,
    val phaseDetection: Boolean = true,
    val speedDetection: Boolean = true,
    val reachDetection: Boolean = true,
    val autoClickerDetection: Boolean = true,
    val killAuraDetection: Boolean = true,
    val scaffoldDetection: Boolean = true,
    val noFallDetection: Boolean = true,
    val timerDetection: Boolean = true,
    val velocityBypassDetection: Boolean = true,
    val packetSpoofingDetection: Boolean = true
)

/**
 * Self-evolving defense configuration
 */
data class AdaptationConfig(
    val enabled: Boolean = true,
    val evolutionInterval: Long = 7200000, // 2 hours
    val syntheticAdversarialSimulation: Boolean = true,
    val modelUpdateFrequency: Long = 3600000, // 1 hour
    val anomalyThresholdAdjustment: Boolean = true,
    val patternLearning: Boolean = true,
    val threatIntelligenceIntegration: Boolean = true,
    val isolatedEnvironment: Boolean = true
)

/**
 * Enforcement configuration
 */
data class EnforcementConfig(
    val enabled: Boolean = true,
    val zeroTolerance: Boolean = true,
    val immediateAction: Boolean = true,
    val warningThreshold: Int = 1,
    val quarantineThreshold: Int = 25,
    val tempBanThreshold: Int = 50,
    val permanentBanThreshold: Int = 100,
    val violationDecay: Long = 300000, // 5 minutes
    val maxViolations: Int = 1000
)

/**
 * Quarantine configuration
 */
data class QuarantineConfig(
    val enabled: Boolean = true,
    val quarantineDuration: Long = 300000, // 5 minutes
    val investigationTimeout: Long = 60000, // 1 minute
    val maxQuarantinedPlayers: Int = 100,
    val isolationLevel: IsolationLevel = IsolationLevel.MEDIUM,
    val monitoringEnabled: Boolean = true,
    val autoRelease: Boolean = false
)

enum class IsolationLevel {
    LOW, MEDIUM, HIGH, MAXIMUM
}

/**
 * Rollback configuration
 */
data class RollbackConfig(
    val enabled: Boolean = true,
    val maxRollbackDistance: Int = 10,
    val rollbackHistory: Int = 100,
    val velocityRollback: Boolean = true,
    val positionRollback: Boolean = true,
    val actionRollback: Boolean = true,
    val rollbackDelay: Long = 100, // 100ms
    val maxRollbacksPerSecond: Int = 20
)

/**
 * Threat intelligence configuration
 */
data class IntelligenceConfig(
    val enabled: Boolean = true,
    val dataRetention: Long = 86400000, // 24 hours
    val threatSharing: Boolean = true,
    val patternAnalysis: Boolean = true,
    val riskAssessment: Boolean = true,
    val threatScoring: Boolean = true,
    val maxThreats: Int = 10000,
    val updateFrequency: Long = 60000 // 1 minute
)

/**
 * Performance monitoring configuration
 */
data class PerformanceConfig(
    val enabled: Boolean = true,
    val monitoringInterval: Long = 1000, // 1 second
    val optimizationThreshold: Double = 0.8,
    val maxCpuUsage: Double = 0.8,
    val maxMemoryUsage: Double = 0.8,
    val gcOptimization: Boolean = true,
    val threadPoolOptimization: Boolean = true,
    val algorithmOptimization: Boolean = true
)

/**
 * Anomaly tracking configuration
 */
data class AnomalyConfig(
    val enabled: Boolean = true,
    val anomalyWindow: Long = 60000, // 1 minute
    val anomalyThreshold: Double = 0.7,
    val statisticalAnalysis: Boolean = true,
    val machineLearning: Boolean = true,
    val realTimeDetection: Boolean = true,
    val maxAnomalies: Int = 1000,
    val falsePositiveReduction: Boolean = true
)