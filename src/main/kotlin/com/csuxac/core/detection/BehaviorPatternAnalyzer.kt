package com.csuxac.core.detection

import com.csuxac.config.BehaviorConfig
import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.sqrt

/**
 * BehaviorPatternAnalyzer - Advanced behavior analysis for cheat detection
 * 
 * Features:
 * - Shannon entropy analysis for click patterns
 * - Human likeness scoring
 * - Pattern recognition for bots and macros
 * - Statistical anomaly detection
 * - Real-time behavior profiling
 */
class BehaviorPatternAnalyzer(
    private val config: BehaviorConfig
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Behavior history tracking
    private val behaviorHistory = mutableMapOf<String, MutableList<BehaviorRecord>>()
    private val entropyScores = mutableMapOf<String, Double>()
    private val humanLikenessScores = mutableMapOf<String, Double>()
    
    /**
     * Analyze player action for behavior anomalies
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
            
            // 3. Human likeness analysis
            val humanViolation = analyzeHumanLikeness(action.playerId, session)
            if (humanViolation != null) {
                violations.add(humanViolation)
                confidence *= 0.8
            }
            
            // 4. Timing analysis
            val timingViolation = analyzeTimingPatterns(action.playerId, session)
            if (timingViolation != null) {
                violations.add(timingViolation)
                confidence *= 0.7
            }
            
            // 5. Action sequence analysis
            val sequenceViolation = analyzeActionSequences(action.playerId, session)
            if (sequenceViolation != null) {
                violations.add(sequenceViolation)
                confidence *= 0.8
            }
            
            // Calculate scores
            val entropyScore = calculateEntropyScore(action.playerId)
            val humanLikeness = calculateHumanLikenessScore(action.playerId)
            val patternType = determineBehaviorPattern(action.playerId, session)
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
                    confidence = 0.9,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.BEHAVIOR_ANOMALY,
                            value = "Analysis error: ${e.message}",
                            confidence = 0.9,
                            description = "Behavior analysis failed due to system error"
                        )
                    ),
                    timestamp = System.currentTimeMillis(),
                    playerId = action.playerId
                )
            )
            
            return BehaviorValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.1,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Analyze entropy for click and action patterns
     */
    private fun analyzeEntropy(playerId: String, session: PlayerSecuritySession): Violation? {
        if (!config.entropyAnalysis) return null
        
        val entropy = calculateEntropyScore(playerId)
        val threshold = config.entropyThreshold
        
        if (entropy < threshold) {
            return Violation(
                type = ViolationType.AUTO_CLICKER,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.STATISTICAL_ANOMALY,
                        value = "Entropy: $entropy, Threshold: $threshold",
                        confidence = 0.9,
                        description = "Low entropy detected - possible bot behavior"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Detect behavior patterns indicative of cheating
     */
    private fun detectBehaviorPatterns(playerId: String, session: PlayerSecuritySession): Violation? {
        if (!config.patternRecognition) return null
        
        val history = behaviorHistory[playerId] ?: return null
        if (history.size < 10) return null
        
        // Check for repetitive patterns
        val patterns = extractPatterns(history)
        val repetitivePatterns = patterns.filter { it.frequency > 0.3 } // More than 30% occurrence
        
        if (repetitivePatterns.isNotEmpty()) {
            val mostRepetitive = repetitivePatterns.maxByOrNull { it.frequency }
            return Violation(
                type = ViolationType.BEHAVIOR_HACK,
                confidence = 0.8,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PATTERN_DETECTION,
                        value = "Pattern: ${mostRepetitive?.pattern}, Frequency: ${mostRepetitive?.frequency}",
                        confidence = 0.8,
                        description = "Repetitive behavior pattern detected"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Analyze human likeness of behavior
     */
    private fun analyzeHumanLikeness(playerId: String, session: PlayerSecuritySession): Violation? {
        if (!config.humanLikeness) return null
        
        val humanScore = calculateHumanLikenessScore(playerId)
        val threshold = config.humanThreshold
        
        if (humanScore < threshold) {
            return Violation(
                type = ViolationType.BEHAVIOR_HACK,
                confidence = 0.85,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.BEHAVIOR_ANOMALY,
                        value = "Human likeness: $humanScore, Threshold: $threshold",
                        confidence = 0.85,
                        description = "Low human likeness score - possible automation"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Analyze timing patterns for anomalies
     */
    private fun analyzeTimingPatterns(playerId: String, session: PlayerSecuritySession): Violation? {
        val history = behaviorHistory[playerId] ?: return null
        if (history.size < 5) return null
        
        val recentActions = history.takeLast(10)
        val intervals = recentActions.zipWithNext().map { (prev, next) ->
            next.timestamp - prev.timestamp
        }
        
        // Check for too-regular timing (bot-like)
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // If timing is too regular, it's suspicious
        if (stdDev < avgInterval * 0.1 && intervals.size > 5) {
            return Violation(
                type = ViolationType.AUTO_CLICKER,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.TIMING_ANOMALY,
                        value = "Timing std dev: $stdDev, Avg: $avgInterval",
                        confidence = 0.9,
                        description = "Too-regular timing detected - possible automation"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Analyze action sequences for suspicious patterns
     */
    private fun analyzeActionSequences(playerId: String, session: PlayerSecuritySession): Violation? {
        val history = behaviorHistory[playerId] ?: return null
        if (history.size < 20) return null
        
        // Check for kill aura patterns
        val killAuraPattern = detectKillAuraPattern(history)
        if (killAuraPattern) {
            return Violation(
                type = ViolationType.KILL_AURA,
                confidence = 0.95,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PATTERN_DETECTION,
                        value = "Kill aura pattern detected",
                        confidence = 0.95,
                        description = "Suspicious attack pattern consistent with kill aura"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        // Check for scaffold patterns
        val scaffoldPattern = detectScaffoldPattern(history)
        if (scaffoldPattern) {
            return Violation(
                type = ViolationType.SCAFFOLD_HACK,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PATTERN_DETECTION,
                        value = "Scaffold pattern detected",
                        confidence = 0.9,
                        description = "Suspicious block placement pattern"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    /**
     * Calculate Shannon entropy for behavior patterns
     */
    private fun calculateEntropyScore(playerId: String): Double {
        val history = behaviorHistory[playerId] ?: return 1.0
        if (history.size < 10) return 1.0
        
        // Group actions by type and calculate entropy
        val actionCounts = history.groupBy { it.actionType }.mapValues { it.value.size }
        val totalActions = history.size
        
        var entropy = 0.0
        for ((_, count) in actionCounts) {
            val probability = count.toDouble() / totalActions
            if (probability > 0) {
                entropy -= probability * log(probability, 2.0)
            }
        }
        
        // Normalize entropy (0 = no randomness, 1 = maximum randomness)
        val maxEntropy = log(actionCounts.size.toDouble(), 2.0)
        val normalizedEntropy = if (maxEntropy > 0) entropy / maxEntropy else 0.0
        
        entropyScores[playerId] = normalizedEntropy
        return normalizedEntropy
    }
    
    /**
     * Calculate human likeness score
     */
    private fun calculateHumanLikenessScore(playerId: String): Double {
        val history = behaviorHistory[playerId] ?: return 1.0
        if (history.size < 20) return 1.0
        
        var score = 1.0
        
        // 1. Timing variance (humans have variable timing)
        val timingScore = calculateTimingVarianceScore(history)
        score *= timingScore
        
        // 2. Action variety (humans don't repeat exact patterns)
        val varietyScore = calculateActionVarietyScore(history)
        score *= varietyScore
        
        // 3. Natural pauses (humans take breaks)
        val pauseScore = calculateNaturalPauseScore(history)
        score *= pauseScore
        
        // 4. Context awareness (humans respond to environment)
        val contextScore = calculateContextAwarenessScore(history)
        score *= contextScore
        
        humanLikenessScores[playerId] = score
        return score
    }
    
    /**
     * Determine behavior pattern type
     */
    private fun determineBehaviorPattern(playerId: String, session: PlayerSecuritySession): BehaviorPattern {
        val entropy = entropyScores[playerId] ?: 1.0
        val humanLikeness = humanLikenessScores[playerId] ?: 1.0
        
        return when {
            entropy < 0.3 && humanLikeness < 0.4 -> BehaviorPattern.SCRIPT
            entropy < 0.5 && humanLikeness < 0.6 -> BehaviorPattern.BOT
            entropy < 0.7 && humanLikeness < 0.8 -> BehaviorPattern.MACRO
            entropy >= 0.7 && humanLikeness >= 0.8 -> BehaviorPattern.HUMAN
            else -> BehaviorPattern.UNKNOWN
        }
    }
    
    /**
     * Calculate anomaly score
     */
    private fun calculateAnomalyScore(
        violationCount: Int,
        entropyScore: Double,
        humanLikeness: Double
    ): Double {
        var score = 0.0
        
        // Base score from violations
        score += violationCount * 0.2
        
        // Adjust for entropy
        if (entropyScore < 0.5) score += 0.3
        
        // Adjust for human likeness
        if (humanLikeness < 0.6) score += 0.4
        
        return min(1.0, score)
    }
    
    /**
     * Record player action for analysis
     */
    private fun recordAction(action: PlayerAction, session: PlayerSecuritySession) {
        val history = behaviorHistory.getOrPut(action.playerId) { mutableListOf() }
        
        val record = BehaviorRecord(
            actionType = action.type,
            timestamp = action.timestamp,
            position = action.position,
            target = action.target,
            metadata = action.metadata
        )
        
        history.add(record)
        
        // Keep only recent history
        if (history.size > config.historySize) {
            history.removeAt(0)
        }
        
        // Update session
        session.updateActivity()
    }
    
    /**
     * Extract behavior patterns from history
     */
    private fun extractPatterns(history: List<BehaviorRecord>): List<BehaviorPattern> {
        val patterns = mutableListOf<BehaviorPattern>()
        
        // Look for 3-action sequences
        for (i in 0 until history.size - 2) {
            val sequence = "${history[i].actionType} → ${history[i + 1].actionType} → ${history[i + 2].actionType}"
            val frequency = history.count { 
                it.actionType == history[i].actionType &&
                it.actionType == history[i + 1].actionType &&
                it.actionType == history[i + 2].actionType
            }.toDouble() / history.size
            
            patterns.add(BehaviorPattern(sequence, frequency))
        }
        
        return patterns
    }
    
    /**
     * Detect kill aura patterns
     */
    private fun detectKillAuraPattern(history: List<BehaviorRecord>): Boolean {
        val recentActions = history.takeLast(20)
        val attacks = recentActions.filter { it.actionType == ActionType.ATTACK }
        
        if (attacks.size < 5) return false
        
        // Check for rapid attacks in different directions
        val attackIntervals = attacks.zipWithNext().map { (prev, next) ->
            next.timestamp - prev.timestamp
        }
        
        val rapidAttacks = attackIntervals.count { it < 100 } // Less than 100ms between attacks
        return rapidAttacks > attacks.size * 0.6 // More than 60% rapid attacks
    }
    
    /**
     * Detect scaffold patterns
     */
    private fun detectScaffoldPattern(history: List<BehaviorRecord>): Boolean {
        val recentActions = history.takeLast(30)
        val blockPlacements = recentActions.filter { it.actionType == ActionType.PLACE_BLOCK }
        
        if (blockPlacements.size < 3) return false
        
        // Check for rapid block placement while moving
        val movements = recentActions.filter { it.actionType == ActionType.MOVE }
        
        if (movements.size < 5) return false
        
        // Scaffold typically involves placing blocks while moving
        val blockPlacementRate = blockPlacements.size.toDouble() / movements.size
        return blockPlacementRate > 0.3 // More than 30% block placement rate
    }
    
    // Helper functions for human likeness scoring
    private fun calculateTimingVarianceScore(history: List<BehaviorRecord>): Double {
        val intervals = history.zipWithNext().map { (prev, next) ->
            next.timestamp - prev.timestamp
        }
        
        if (intervals.isEmpty()) return 1.0
        
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Higher variance = more human-like
        return min(1.0, stdDev / avgInterval)
    }
    
    private fun calculateActionVarietyScore(history: List<BehaviorRecord>): Double {
        val uniqueActions = history.map { it.actionType }.distinct().size
        val totalActions = history.size
        
        return uniqueActions.toDouble() / totalActions
    }
    
    private fun calculateNaturalPauseScore(history: List<BehaviorRecord>): Double {
        val intervals = history.zipWithNext().map { (prev, next) ->
            next.timestamp - prev.timestamp
        }
        
        val longPauses = intervals.count { it > 1000 } // Pauses longer than 1 second
        val pauseRatio = longPauses.toDouble() / intervals.size
        
        // Humans typically have some long pauses
        return if (pauseRatio in 0.1..0.3) 1.0 else 0.5
    }
    
    private fun calculateContextAwarenessScore(history: List<BehaviorRecord>): Double {
        // Simplified context awareness - would be more sophisticated in real implementation
        val contextSwitches = history.zipWithNext().count { (prev, next) ->
            prev.actionType.category != next.actionType.category
        }
        
        val totalTransitions = history.size - 1
        val contextRatio = contextSwitches.toDouble() / totalTransitions
        
        // Higher context switching = more human-like
        return min(1.0, contextRatio * 2)
    }
    
    // Data classes
    private data class BehaviorRecord(
        val actionType: ActionType,
        val timestamp: Long,
        val position: Vector3D,
        val target: ActionTarget?,
        val metadata: Map<String, Any>
    )
    
    private data class BehaviorPattern(
        val pattern: String,
        val frequency: Double
    )
}