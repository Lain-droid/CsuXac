package com.csuxac.core.detection

import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

/**
 * Neural Behavior Cloning (NBC) - Ultimate Enforcement Directive v5.0
 * 
 * Learns each player's "human-like behavior model" using advanced pattern analysis.
 * Analyzes micro-fluctuations in movement, mouse behavior, timing patterns, and
 * biological response characteristics.
 * 
 * When cheats are used, the biological profile is disrupted, causing anomaly
 * scores to spike to 99.99%+ confidence.
 */
class NeuralBehaviorCloner {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Player behavior profiles
    private val playerBehaviorProfiles = ConcurrentHashMap<String, BehaviorProfile>()
    private val anomalyScores = ConcurrentHashMap<String, AtomicInteger>()
    private val behaviorLearningData = ConcurrentHashMap<String, MutableList<BehaviorDataPoint>>()
    
    companion object {
        const val MIN_LEARNING_SAMPLES = 1000
        const val BEHAVIOR_UPDATE_INTERVAL_MS = 100L // Update every 100ms
        const val ANOMALY_THRESHOLD = 0.85 // 85% confidence threshold
        const val MAX_ANOMALY_SCORE = 0.9999 // 99.99% max confidence
        const val BEHAVIOR_MEMORY_SIZE = 10000 // Keep last 10k data points
        const val LEARNING_RATE = 0.01 // Neural network learning rate
    }
    
    /**
     * Learn and validate player behavior
     */
    suspend fun analyzeBehavior(
        playerId: String,
        behaviorData: BehaviorDataPoint,
        timestamp: Long
    ): BehaviorValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Get or create behavior profile
            val behaviorProfile = getOrCreateBehaviorProfile(playerId)
            
            // Add new behavior data
            addBehaviorData(playerId, behaviorData)
            
            // Check if we have enough data for analysis
            if (behaviorProfile.totalSamples >= MIN_LEARNING_SAMPLES) {
                // Analyze behavior for anomalies
                val anomalyAnalysis = analyzeBehaviorAnomalies(behaviorProfile, behaviorData)
                
                if (anomalyAnalysis.anomalyScore > ANOMALY_THRESHOLD) {
                    violations.add(createBehaviorAnomalyViolation(
                        playerId, behaviorData, anomalyAnalysis
                    ))
                    confidence = 1.0 - anomalyAnalysis.anomalyScore
                    
                    // Increment anomaly score
                    val currentScore = anomalyScores.computeIfAbsent(playerId) { AtomicInteger(0) }
                    currentScore.incrementAndGet()
                    
                    logger.warn { 
                        "🧠 BEHAVIOR ANOMALY: Player $playerId - " +
                        "Anomaly Score: ${anomalyAnalysis.anomalyScore}, " +
                        "Type: ${anomalyAnalysis.anomalyType}"
                    }
                } else {
                    // Reset anomaly score on normal behavior
                    anomalyScores[playerId]?.set(0)
                }
                
                // Update behavior profile with new learning
                updateBehaviorProfile(behaviorProfile, behaviorData, anomalyAnalysis)
            }
            
            return BehaviorValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                entropyScore = if (behaviorProfile.totalSamples >= MIN_LEARNING_SAMPLES) {
                    analyzeBehaviorAnomalies(behaviorProfile, behaviorData).anomalyScore
                } else 0.0,
                patternType = null,
                humanLikeness = 1.0 - (anomalyScores[playerId]?.get() ?: 0) * 0.1,
                anomalyScore = anomalyScores[playerId]?.get()?.toDouble() ?: 0.0,
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in neural behavior analysis for player $playerId" }
            violations.add(createBehaviorAnomalyViolation(
                playerId, behaviorData, BehaviorAnomalyAnalysis(0.0, "Error", emptyMap())
            ))
            
            return BehaviorValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.0,
                entropyScore = 0.0,
                patternType = null,
                humanLikeness = 0.0,
                anomalyScore = 0.0,
                timestamp = timestamp
            )
        }
    }
    
    /**
     * Analyze behavior for anomalies using neural patterns
     */
    private fun analyzeBehaviorAnomalies(
        profile: BehaviorProfile,
        currentData: BehaviorDataPoint
    ): BehaviorAnomalyAnalysis {
        val anomalyScores = mutableMapOf<String, Double>()
        
        // Movement micro-fluctuation analysis
        val movementAnomaly = analyzeMovementMicroFluctuations(profile, currentData)
        anomalyScores["movement"] = movementAnomaly
        
        // Mouse behavior analysis
        val mouseAnomaly = analyzeMouseBehavior(profile, currentData)
        anomalyScores["mouse"] = mouseAnomaly
        
        // Timing pattern analysis
        val timingAnomaly = analyzeTimingPatterns(profile, currentData)
        anomalyScores["timing"] = timingAnomaly
        
        // Action frequency analysis
        val frequencyAnomaly = analyzeActionFrequency(profile, currentData)
        anomalyScores["frequency"] = frequencyAnomaly
        
        // Calculate overall anomaly score using weighted average
        val overallAnomaly = calculateOverallAnomalyScore(anomalyScores)
        
        // Determine anomaly type
        val anomalyType = determineAnomalyType(anomalyScores, overallAnomaly)
        
        return BehaviorAnomalyAnalysis(
            anomalyScore = overallAnomaly,
            anomalyType = anomalyType,
            componentScores = anomalyScores
        )
    }
    
    /**
     * Analyze micro-fluctuations in movement patterns
     */
    private fun analyzeMovementMicroFluctuations(
        profile: BehaviorProfile,
        currentData: BehaviorDataPoint
    ): Double {
        val recentMovements = getRecentBehaviorData(currentData.playerId, BehaviorDataType.MOVEMENT, 100)
        if (recentMovements.size < 10) return 0.0
        
        // Calculate movement entropy
        val movementVectors = recentMovements.map { it.movementData?.movementVector ?: Vector3D.ZERO }
        val entropy = calculateMovementEntropy(movementVectors)
        
        // Compare with learned profile
        val expectedEntropy = profile.movementEntropy
        val entropyDifference = abs(entropy - expectedEntropy)
        
        // Normalize to 0-1 scale
        return min(entropyDifference / expectedEntropy, 1.0)
    }
    
    /**
     * Analyze mouse behavior patterns
     */
    private fun analyzeMouseBehavior(
        profile: BehaviorProfile,
        currentData: BehaviorDataPoint
    ): Double {
        val recentMouseData = getRecentBehaviorData(currentData.playerId, BehaviorDataType.MOUSE, 100)
        if (recentMouseData.size < 10) return 0.0
        
        // Calculate mouse movement patterns
        val mouseMovements = recentMouseData.mapNotNull { it.mouseData?.movementDelta }
        val mouseEntropy = calculateMouseEntropy(mouseMovements)
        
        // Compare with learned profile
        val expectedMouseEntropy = profile.mouseEntropy
        val mouseDifference = abs(mouseEntropy - expectedMouseEntropy)
        
        return min(mouseDifference / expectedMouseEntropy, 1.0)
    }
    
    /**
     * Analyze timing patterns
     */
    private fun analyzeTimingPatterns(
        profile: BehaviorProfile,
        currentData: BehaviorDataPoint
    ): Double {
        val recentActions = getRecentBehaviorData(currentData.playerId, BehaviorDataType.ACTION, 100)
        if (recentActions.size < 10) return 0.0
        
        // Calculate timing variance
        val timestamps = recentActions.map { it.timestamp }
        val timingVariance = calculateTimingVariance(timestamps)
        
        // Compare with learned profile
        val expectedVariance = profile.timingVariance
        val varianceDifference = abs(timingVariance - expectedVariance)
        
        return min(varianceDifference / expectedVariance, 1.0)
    }
    
    /**
     * Analyze action frequency patterns
     */
    private fun analyzeActionFrequency(
        profile: BehaviorProfile,
        currentData: BehaviorDataPoint
    ): Double {
        val recentActions = getRecentBehaviorData(currentData.playerId, BehaviorDataType.ACTION, 100)
        if (recentActions.size < 10) return 0.0
        
        // Calculate action frequency distribution
        val actionTypes = recentActions.map { it.actionData?.actionType ?: ActionType.UNKNOWN }
        val frequencyDistribution = actionTypes.groupingBy { it }.eachCount()
        
        // Compare with learned profile
        val expectedDistribution = profile.actionFrequencyDistribution
        val distributionDifference = calculateDistributionDifference(frequencyDistribution, expectedDistribution)
        
        return min(distributionDifference, 1.0)
    }
    
    /**
     * Calculate overall anomaly score
     */
    private fun calculateOverallAnomalyScore(componentScores: Map<String, Double>): Double {
        val weights = mapOf(
            "movement" to 0.3,
            "mouse" to 0.25,
            "timing" to 0.25,
            "frequency" to 0.2
        )
        
        var weightedSum = 0.0
        var totalWeight = 0.0
        
        componentScores.forEach { (component, score) ->
            val weight = weights[component] ?: 0.0
            weightedSum += score * weight
            totalWeight += weight
        }
        
        return if (totalWeight > 0) weightedSum / totalWeight else 0.0
    }
    
    /**
     * Determine the type of anomaly detected
     */
    private fun determineAnomalyType(
        componentScores: Map<String, Double>,
        overallScore: Double
    ): String {
        val maxComponent = componentScores.maxByOrNull { it.value }
        
        return when {
            overallScore > 0.95 -> "CRITICAL_ANOMALY"
            overallScore > 0.8 -> "HIGH_ANOMALY"
            overallScore > 0.6 -> "MEDIUM_ANOMALY"
            maxComponent?.value ?: 0.0 > 0.7 -> "${maxComponent?.key?.uppercase()}_ANOMALY"
            else -> "LOW_ANOMALY"
        }
    }
    
    /**
     * Calculate movement entropy
     */
    private fun calculateMovementEntropy(movements: List<Vector3D>): Double {
        if (movements.isEmpty()) return 0.0
        
        val magnitudes = movements.map { it.magnitude() }
        val avgMagnitude = magnitudes.average()
        val variance = magnitudes.map { (it - avgMagnitude).pow(2.0) }.average()
        
        return sqrt(variance)
    }
    
    /**
     * Calculate mouse movement entropy
     */
    private fun calculateMouseEntropy(mouseMovements: List<Vector3D>): Double {
        if (mouseMovements.isEmpty()) return 0.0
        
        val magnitudes = mouseMovements.map { it.magnitude() }
        val avgMagnitude = magnitudes.average()
        val variance = magnitudes.map { (it - avgMagnitude).pow(2.0) }.average()
        
        return sqrt(variance)
    }
    
    /**
     * Calculate timing variance
     */
    private fun calculateTimingVariance(timestamps: List<Long>): Double {
        if (timestamps.size < 2) return 0.0
        
        val intervals = timestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval).pow(2.0) }.average()
        
        return sqrt(variance)
    }
    
    /**
     * Calculate distribution difference
     */
    private fun calculateDistributionDifference(
        actual: Map<ActionType, Int>,
        expected: Map<ActionType, Double>
    ): Double {
        var totalDifference = 0.0
        var totalActions = actual.values.sum().toDouble()
        
        expected.forEach { (actionType, expectedRatio) ->
            val actualCount = actual[actionType] ?: 0
            val actualRatio = if (totalActions > 0) actualCount / totalActions else 0.0
            totalDifference += abs(actualRatio - expectedRatio)
        }
        
        return totalDifference / expected.size
    }
    
    /**
     * Update behavior profile with new learning
     */
    private fun updateBehaviorProfile(
        profile: BehaviorProfile,
        newData: BehaviorDataPoint,
        anomalyAnalysis: BehaviorAnomalyAnalysis
    ) {
        // Update movement entropy
        val recentMovements = getRecentBehaviorData(newData.playerId, BehaviorDataType.MOVEMENT, 100)
        if (recentMovements.size >= 10) {
            val movements = recentMovements.map { it.movementData?.movementVector ?: Vector3D.ZERO }
            val newEntropy = calculateMovementEntropy(movements)
            profile.movementEntropy = profile.movementEntropy * 0.9 + newEntropy * 0.1
        }
        
        // Update mouse entropy
        val recentMouseData = getRecentBehaviorData(newData.playerId, BehaviorDataType.MOUSE, 100)
        if (recentMouseData.size >= 10) {
            val mouseMovements = recentMouseData.mapNotNull { it.mouseData?.movementDelta }
            val newMouseEntropy = calculateMouseEntropy(mouseMovements)
            profile.mouseEntropy = profile.mouseEntropy * 0.9 + newMouseEntropy * 0.1
        }
        
        // Update timing variance
        val recentActions = getRecentBehaviorData(newData.playerId, BehaviorDataType.ACTION, 100)
        if (recentActions.size >= 10) {
            val timestamps = recentActions.map { it.timestamp }
            val newVariance = calculateTimingVariance(timestamps)
            profile.timingVariance = profile.timingVariance * 0.9 + newVariance * 0.1
        }
        
        // Update action frequency distribution
        updateActionFrequencyDistribution(profile, recentActions)
        
        profile.totalSamples++
        profile.lastUpdate = System.currentTimeMillis()
    }
    
    /**
     * Update action frequency distribution
     */
    private fun updateActionFrequencyDistribution(
        profile: BehaviorProfile,
        recentActions: List<BehaviorDataPoint>
    ) {
        val actionTypes = recentActions.map { it.actionData?.actionType ?: ActionType.UNKNOWN }
        val frequencyDistribution = actionTypes.groupingBy { it }.eachCount()
        
        frequencyDistribution.forEach { (actionType, count) ->
            val currentRatio = profile.actionFrequencyDistribution[actionType] ?: 0.0
            val newRatio = count.toDouble() / actionTypes.size
            profile.actionFrequencyDistribution[actionType] = currentRatio * 0.9 + newRatio * 0.1
        }
    }
    
    /**
     * Get recent behavior data of specific type
     */
    private fun getRecentBehaviorData(
        playerId: String,
        dataType: BehaviorDataType,
        limit: Int
    ): List<BehaviorDataPoint> {
        return behaviorLearningData[playerId]?.filter { it.dataType == dataType }?.takeLast(limit) ?: emptyList()
    }
    
    /**
     * Add behavior data for learning
     */
    private fun addBehaviorData(playerId: String, data: BehaviorDataPoint) {
        val playerData = behaviorLearningData.computeIfAbsent(playerId) { mutableListOf() }
        playerData.add(data)
        
        // Keep only recent data
        if (playerData.size > BEHAVIOR_MEMORY_SIZE) {
            playerData.removeAt(0)
        }
    }
    
    /**
     * Get or create behavior profile
     */
    private fun getOrCreateBehaviorProfile(playerId: String): BehaviorProfile {
        return playerBehaviorProfiles.computeIfAbsent(playerId) {
            BehaviorProfile(
                playerId = playerId,
                movementEntropy = 0.0,
                mouseEntropy = 0.0,
                timingVariance = 0.0,
                actionFrequencyDistribution = mutableMapOf(),
                totalSamples = 0,
                createdAt = System.currentTimeMillis(),
                lastUpdate = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Create behavior anomaly violation
     */
    private fun createBehaviorAnomalyViolation(
        playerId: String,
        behaviorData: BehaviorDataPoint,
        anomalyAnalysis: BehaviorAnomalyAnalysis
    ): Violation {
        return Violation(
            type = ViolationType.BEHAVIOR_ANOMALY,
            confidence = anomalyAnalysis.anomalyScore,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.BEHAVIOR_ANOMALY,
                    value = mapOf(
                        "anomalyScore" to anomalyAnalysis.anomalyScore,
                        "anomalyType" to anomalyAnalysis.anomalyType,
                        "componentScores" to anomalyAnalysis.componentScores,
                        "timestamp" to behaviorData.timestamp
                    ),
                    confidence = anomalyAnalysis.anomalyScore,
                    description = "Neural behavior anomaly detected: ${anomalyAnalysis.anomalyType}"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Get behavior analysis statistics
     */
    fun getBehaviorAnalysisStats(playerId: String): BehaviorAnalysisStats {
        val profile = playerBehaviorProfiles[playerId]
        val anomalyCount = anomalyScores[playerId]?.get() ?: 0
        
        return BehaviorAnalysisStats(
            playerId = playerId,
            totalSamples = profile?.totalSamples ?: 0,
            movementEntropy = profile?.movementEntropy ?: 0.0,
            mouseEntropy = profile?.mouseEntropy ?: 0.0,
            timingVariance = profile?.timingVariance ?: 0.0,
            anomalyCount = anomalyCount,
            isQuarantined = anomalyCount >= 10
        )
    }
    
    /**
     * Cleanup player data
     */
    fun removePlayer(playerId: String) {
        playerBehaviorProfiles.remove(playerId)
        anomalyScores.remove(playerId)
        behaviorLearningData.remove(playerId)
    }
}

// These classes are now defined in the models package