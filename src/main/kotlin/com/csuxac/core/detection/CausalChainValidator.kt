package com.csuxac.core.detection

import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Causal Chain Validation (CCV) - Ultimate Enforcement Directive v5.0
 * 
 * Validates that every action is logically connected to previous actions.
 * Creates a causal graph for each player to detect impossible sequences
 * like damage without attack, item pickup without block break, etc.
 * 
 * Time Logic: A result cannot occur before its cause.
 */
class CausalChainValidator {
    private val logger = defaultLogger()
    
    // Causal graph tracking per player
    private val playerCausalGraphs = ConcurrentHashMap<String, CausalGraph>()
    private val causalViolationCount = ConcurrentHashMap<String, AtomicInteger>()
    private val lastActionTimestamps = ConcurrentHashMap<String, Long>()
    
    companion object {
        const val MAX_CAUSAL_CHAIN_LENGTH = 1000
        const val CAUSAL_TIMEOUT_MS = 30000L // 30 seconds
        const val MAX_CAUSAL_VIOLATIONS = 10
        const val CAUSAL_CHAIN_CLEANUP_INTERVAL = 1000L // Cleanup every 1000 actions
    }
    
    /**
     * Validate action causality
     */
    fun validateCausality(
        playerId: String,
        action: PlayerAction,
        timestamp: Long
    ): CausalValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Get or create causal graph
            val causalGraph = getOrCreateCausalGraph(playerId)
            
            // Check if action is causally valid
            val causalValidation = validateActionCausality(action, causalGraph, timestamp)
            
            if (!causalValidation.isValid) {
                violations.addAll(causalValidation.violations)
                confidence = 0.0
                
                // Increment violation count
                val violationCount = causalViolationCount.computeIfAbsent(playerId) { AtomicInteger(0) }
                violationCount.incrementAndGet()
                
                logger.warn { 
                    "⏳ CAUSAL VIOLATION: Player $playerId - " +
                    "Action: ${action.type}, Reason: ${causalValidation.reason}"
                }
            }
            
            // Add action to causal graph
            addActionToGraph(causalGraph, action, timestamp)
            
            // Cleanup old actions if needed
            if (causalGraph.actions.size > MAX_CAUSAL_CHAIN_LENGTH) {
                cleanupOldActions(causalGraph, timestamp)
            }
            
            // Update last action timestamp
            lastActionTimestamps[playerId] = timestamp
            
            return CausalValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                action = action,
                causalChain = causalGraph.actions.takeLast(10), // Last 10 actions
                validationDetails = causalValidation,
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in causal chain validation for player $playerId" }
            violations.add(createCausalViolation(
                playerId, action, "Validation error: ${e.message}"
            ))
            
            return CausalValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.0,
                action = action,
                causalChain = emptyList(),
                validationDetails = CausalActionValidation(false, emptyList(), "Error"),
                timestamp = timestamp
            )
        }
    }
    
    /**
     * Validate if an action is causally possible
     */
    private fun validateActionCausality(
        action: PlayerAction,
        causalGraph: CausalGraph,
        timestamp: Long
    ): CausalActionValidation {
        val violations = mutableListOf<Violation>()
        var isValid = true
        var reason = "Valid"
        
        // Check for impossible actions without prerequisites
        when (action.type) {
            ActionType.ITEM_PICKUP -> {
                if (!hasPrerequisiteAction(causalGraph, ActionType.BREAK_BLOCK, timestamp)) {
                    violations.add(createCausalViolation(
                        action.playerId, action, "Item pickup without block break"
                    ))
                    isValid = false
                    reason = "Missing prerequisite: BREAK_BLOCK"
                }
            }
            
            ActionType.CRAFTING -> {
                if (!hasPrerequisiteAction(causalGraph, ActionType.ITEM_PICKUP, timestamp)) {
                    violations.add(createCausalViolation(
                        action.playerId, action, "Crafting without item pickup"
                    ))
                    isValid = false
                    reason = "Missing prerequisite: ITEM_PICKUP"
                }
            }
            
            ActionType.DAMAGE_DEALT -> {
                if (!hasPrerequisiteAction(causalGraph, ActionType.ATTACK, timestamp)) {
                    violations.add(createCausalViolation(
                        action.playerId, action, "Damage dealt without attack"
                    ))
                    isValid = false
                    reason = "Missing prerequisite: ATTACK"
                }
            }
            
            ActionType.DEATH -> {
                if (!hasPrerequisiteAction(causalGraph, ActionType.DAMAGE_DEALT, timestamp)) {
                    violations.add(createCausalViolation(
                        action.playerId, action, "Death without damage"
                    ))
                    isValid = false
                    reason = "Missing prerequisite: DAMAGE_DEALT"
                }
            }
            
            ActionType.BLOCK_PLACE -> {
                if (!hasPrerequisiteAction(causalGraph, ActionType.ITEM_PICKUP, timestamp)) {
                    violations.add(createCausalViolation(
                        action.playerId, action, "Block place without item pickup"
                    ))
                    isValid = false
                    reason = "Missing prerequisite: ITEM_PICKUP"
                }
            }
            
            ActionType.MOVE -> {
                // Movement is always causally valid as it's continuous
                // But check for impossible movement patterns
                if (causalGraph.actions.isNotEmpty()) {
                    val lastAction = causalGraph.actions.last()
                    if (lastAction.type == ActionType.TELEPORT && 
                        timestamp - lastAction.timestamp < 100) {
                        // Teleport followed immediately by movement is suspicious
                        violations.add(createCausalViolation(
                            action.playerId, action, "Movement immediately after teleport"
                        ))
                        isValid = false
                        reason = "Impossible movement pattern"
                    }
                }
            }
            
            else -> {
                // Other actions are generally valid
            }
        }
        
        // Check for temporal impossibilities
        if (causalGraph.actions.isNotEmpty()) {
            val lastAction = causalGraph.actions.last()
            if (timestamp < lastAction.timestamp) {
                violations.add(createCausalViolation(
                    action.playerId, action, "Action timestamp before previous action"
                ))
                isValid = false
                reason = "Temporal impossibility: action before cause"
            }
        }
        
        // Check for action frequency anomalies
        if (isActionFrequencyAnomalous(action, causalGraph, timestamp)) {
            violations.add(createCausalViolation(
                action.playerId, action, "Abnormal action frequency"
            ))
            isValid = false
            reason = "Action frequency anomaly"
        }
        
        return CausalActionValidation(isValid, violations, reason)
    }
    
    /**
     * Check if player has performed a prerequisite action
     */
    private fun hasPrerequisiteAction(
        causalGraph: CausalGraph,
        prerequisiteType: ActionType,
        currentTimestamp: Long
    ): Boolean {
        val cutoffTime = currentTimestamp - CAUSAL_TIMEOUT_MS
        
        return causalGraph.actions.any { action ->
            action.type == prerequisiteType && 
            action.timestamp >= cutoffTime
        }
    }
    
    /**
     * Check for abnormal action frequency
     */
    private fun isActionFrequencyAnomalous(
        action: PlayerAction,
        causalGraph: CausalGraph,
        timestamp: Long
    ): Boolean {
        val recentActions = causalGraph.actions.filter { 
            it.type == action.type && 
            timestamp - it.timestamp < 1000 // Last second
        }
        
        return when (action.type) {
            ActionType.ATTACK -> recentActions.size > 20 // Max 20 attacks per second
            ActionType.BREAK_BLOCK -> recentActions.size > 10 // Max 10 block breaks per second
            ActionType.ITEM_PICKUP -> recentActions.size > 50 // Max 50 pickups per second
            ActionType.MOVE -> recentActions.size > 100 // Max 100 movements per second
            else -> false
        }
    }
    
    /**
     * Add action to causal graph
     */
    private fun addActionToGraph(
        causalGraph: CausalGraph,
        action: PlayerAction,
        timestamp: Long
    ) {
        causalGraph.actions.add(action)
        
        // Create causal relationships
        if (causalGraph.actions.size > 1) {
            val previousAction = causalGraph.actions[causalGraph.actions.size - 2]
            val relationship = CausalRelationship(
                cause = previousAction,
                effect = action,
                timestamp = timestamp
            )
            causalGraph.relationships.add(relationship)
        }
    }
    
    /**
     * Cleanup old actions from causal graph
     */
    private fun cleanupOldActions(causalGraph: CausalGraph, currentTimestamp: Long) {
        val cutoffTime = currentTimestamp - CAUSAL_TIMEOUT_MS
        
        // Remove old actions
        causalGraph.actions.removeAll { it.timestamp < cutoffTime }
        
        // Remove old relationships
        causalGraph.relationships.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * Get or create causal graph for player
     */
    private fun getOrCreateCausalGraph(playerId: String): CausalGraph {
        return playerCausalGraphs.computeIfAbsent(playerId) {
            CausalGraph(
                playerId = playerId,
                actions = mutableListOf(),
                relationships = mutableListOf(),
                createdAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Create causal violation
     */
    private fun createCausalViolation(
        playerId: String,
        action: PlayerAction,
        reason: String
    ): Violation {
        return Violation(
            type = ViolationType.CAUSAL_VIOLATION,
            confidence = 0.9,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.CAUSAL_ANOMALY,
                    value = mapOf(
                        "action" to action.type.name,
                        "reason" to reason,
                        "timestamp" to action.timestamp,
                        "playerId" to playerId
                    ),
                    confidence = 0.9,
                    description = "Causal chain violation: $reason"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Get causal chain statistics
     */
    fun getCausalChainStats(playerId: String): CausalChainStats {
        val causalGraph = playerCausalGraphs[playerId]
        val violationCount = causalViolationCount[playerId]?.get() ?: 0
        val lastAction = lastActionTimestamps[playerId] ?: 0L
        
        return CausalChainStats(
            playerId = playerId,
            totalActions = causalGraph?.actions?.size ?: 0,
            totalRelationships = causalGraph?.relationships?.size ?: 0,
            causalViolations = violationCount,
            lastActionTime = lastAction,
            isQuarantined = violationCount >= MAX_CAUSAL_VIOLATIONS
        )
    }
    
    /**
     * Cleanup player data
     */
    fun removePlayer(playerId: String) {
        playerCausalGraphs.remove(playerId)
        causalViolationCount.remove(playerId)
        lastActionTimestamps.remove(playerId)
    }
}

/**
 * Causal graph for tracking action relationships
 */
data class CausalGraph(
    val playerId: String,
    val actions: MutableList<PlayerAction>,
    val relationships: MutableList<CausalRelationship>,
    val createdAt: Long
)

/**
 * Causal relationship between actions
 */
data class CausalRelationship(
    val cause: PlayerAction,
    val effect: PlayerAction,
    val timestamp: Long
)

// CausalActionValidation is now defined in the models package

// These classes are now defined in the models package