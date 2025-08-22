package com.csuxac.core.detection

import com.csuxac.config.BehaviorConfig
import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * BehaviorPatternAnalyzer - Simplified behavior analysis for anti-cheat
 */
class BehaviorPatternAnalyzer(
    private val config: BehaviorConfig
) {
    private val logger = defaultLogger()
    
    // Player behavior tracking
    private val behaviorHistory = ConcurrentHashMap<String, MutableList<ActionType>>()
    private val entropyScores = ConcurrentHashMap<String, Double>()
    private val humanLikenessScores = ConcurrentHashMap<String, Double>()
    
    /**
     * Analyze player action for suspicious behavior
     */
    suspend fun analyzeAction(
        action: PlayerAction,
        session: PlayerSecuritySession
    ): BehaviorValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Record the action
            recordAction(action, session)
            
            // 1. Entropy analysis
            val entropyViolation = analyzeEntropy(action.playerId, session)
            if (entropyViolation != null) {
                violations.add(entropyViolation)
                confidence *= 0.6
            }
            
            // 2. Pattern recognition
            val patternViolation = detectBehaviorPatterns(action.playerId, session)
            if (patternViolation != null) {
                violations.add(patternViolation)
                confidence *= 0.7
            }
            
            // Calculate scores
            val entropyScore = calculateEntropyScore(action.playerId)
            val humanLikeness = calculateHumanLikenessScore(action.playerId)
            val patternType: com.csuxac.core.models.BehaviorPattern? = null
            val anomalyScore = calculateAnomalyScore(violations.size, entropyScore, humanLikeness)
            
            return BehaviorValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                timestamp = System.currentTimeMillis(),
                entropyScore = entropyScore,
                patternType = patternType,
                humanLikeness = humanLikeness,
                anomalyScore = anomalyScore
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error during behavior analysis for player ${action.playerId}" }
            violations.add(
                Violation(
                    type = ViolationType.BEHAVIOR_HACK,
                    confidence = 0.5,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.BEHAVIOR_ANOMALY,
                            value = "Analysis error: ${e.message}",
                            confidence = 0.5,
                            description = "Error during behavior analysis"
                        )
                    ),
                    timestamp = System.currentTimeMillis(),
                    playerId = action.playerId
                )
            )
            
            return BehaviorValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.5,
                timestamp = System.currentTimeMillis(),
                entropyScore = 0.0,
                patternType = null,
                humanLikeness = 0.0,
                anomalyScore = 1.0
            )
        }
    }
    
    /**
     * Record player action for behavior tracking
     */
    private fun recordAction(action: PlayerAction, session: PlayerSecuritySession) {
        val history = behaviorHistory.computeIfAbsent(action.playerId) { mutableListOf() }
        history.add(action.type)
        
        // Keep only recent history
        if (history.size > config.historySize) {
            history.removeAt(0)
        }
        
        // Update session
        session.updateActivity()
    }
    
    /**
     * Analyze action entropy for bot detection
     */
    private fun analyzeEntropy(playerId: String, session: PlayerSecuritySession): Violation? {
        val history = behaviorHistory[playerId] ?: return null
        if (history.size < 10) return null
        
        val entropyScore = calculateEntropyScore(playerId)
        
        if (entropyScore < config.entropyThreshold) {
            return Violation(
                type = ViolationType.BEHAVIOR_HACK,
                confidence = 0.8,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.BEHAVIOR_ANOMALY,
                        value = "Low entropy score: $entropyScore",
                        confidence = 0.8,
                        description = "Suspicious behavior pattern detected (low entropy)"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Detect repetitive behavior patterns
     */
    private fun detectBehaviorPatterns(playerId: String, session: PlayerSecuritySession): Violation? {
        val history = behaviorHistory[playerId] ?: return null
        if (history.size < 10) return null
        
        // Check for repetitive patterns (simplified)
        if (history.size > 20) {
            val uniqueTypes = history.distinct()
            val repetitionRatio = history.size.toDouble() / uniqueTypes.size
            
            if (repetitionRatio > 3.0) { // More than 3x repetition
                return Violation(
                    type = ViolationType.BEHAVIOR_HACK,
                    confidence = 0.8,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.PATTERN_DETECTION,
                            value = "Repetition ratio: $repetitionRatio",
                            confidence = 0.8,
                            description = "Repetitive behavior pattern detected"
                        )
                    ),
                    timestamp = System.currentTimeMillis(),
                    playerId = playerId
                )
            }
        }
        
        return null
    }
    
    /**
     * Calculate entropy score for player actions
     */
    private fun calculateEntropyScore(playerId: String): Double {
        val history = behaviorHistory[playerId] ?: return 1.0
        if (history.size < 5) return 1.0
        
        val frequencies = history.groupingBy { it }.eachCount()
        val total = history.size.toDouble()
        
        var entropy = 0.0
        for (count in frequencies.values) {
            val probability = count / total
            entropy -= probability * kotlin.math.ln(probability)
        }
        
        val maxEntropy = kotlin.math.ln(frequencies.size.toDouble())
        val normalizedEntropy = if (maxEntropy > 0) entropy / maxEntropy else 0.0
        
        entropyScores[playerId] = normalizedEntropy
        return normalizedEntropy
    }
    
    /**
     * Calculate human likeness score
     */
    private fun calculateHumanLikenessScore(playerId: String): Double {
        val history = behaviorHistory[playerId] ?: return 1.0
        if (history.size < 10) return 1.0
        
        // Simple heuristic: variety of actions and natural pauses
        val uniqueActions = history.distinct().size
        val varietyScore = kotlin.math.min(1.0, uniqueActions / 5.0)
        
        // Check for natural variation
        val variationScore = if (history.size > 1) {
            val changes = history.zipWithNext { a, b -> a != b }.count { it }
            kotlin.math.min(1.0, changes.toDouble() / (history.size - 1))
        } else 1.0
        
        val humanLikeness = (varietyScore + variationScore) / 2.0
        humanLikenessScores[playerId] = humanLikeness
        return humanLikeness
    }
    
    /**
     * Calculate anomaly score based on violations and behavior metrics
     */
    private fun calculateAnomalyScore(
        violationCount: Int,
        entropyScore: Double,
        humanLikeness: Double
    ): Double {
        val violationWeight = kotlin.math.min(1.0, violationCount * 0.3)
        val entropyWeight = 1.0 - entropyScore
        val humanliknessWeight = 1.0 - humanLikeness
        
        return (violationWeight + entropyWeight + humanliknessWeight) / 3.0
    }
    
    /**
     * Get player behavior statistics
     */
    fun getPlayerStats(playerId: String): Map<String, Any> {
        val history = behaviorHistory[playerId]
        return mapOf(
            "actionCount" to (history?.size ?: 0),
            "uniqueActions" to (history?.distinct()?.size ?: 0),
            "entropyScore" to (entropyScores[playerId] ?: 0.0),
            "humanLikeness" to (humanLikenessScores[playerId] ?: 1.0)
        )
    }
}