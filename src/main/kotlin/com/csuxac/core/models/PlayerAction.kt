package com.csuxac.core.models

import java.time.Instant

/**
 * Represents a player action that needs to be analyzed for cheat detection
 */
data class PlayerAction(
    val playerId: String,
    val type: ActionType,
    val timestamp: Long,
    val position: Vector3D,
    val target: ActionTarget? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    val age: Long get() = System.currentTimeMillis() - timestamp
    val isRecent: Boolean get() = age < 1000 // 1 second
}

enum class ActionType(val category: ActionCategory, val riskLevel: RiskLevel) {
    // Movement actions
    MOVE(ActionCategory.MOVEMENT, RiskLevel.LOW),
    JUMP(ActionCategory.MOVEMENT, RiskLevel.MEDIUM),
    SPRINT(ActionCategory.MOVEMENT, RiskLevel.LOW),
    SNEAK(ActionCategory.MOVEMENT, RiskLevel.LOW),
    FLY(ActionCategory.MOVEMENT, RiskLevel.HIGH),
    PHASE(ActionCategory.MOVEMENT, RiskLevel.CRITICAL),
    
    // Combat actions
    ATTACK(ActionCategory.COMBAT, RiskLevel.MEDIUM),
    BLOCK(ActionCategory.COMBAT, RiskLevel.LOW),
    BOW_DRAW(ActionCategory.COMBAT, RiskLevel.LOW),
    BOW_RELEASE(ActionCategory.COMBAT, RiskLevel.MEDIUM),
    
    // Block interactions
    PLACE_BLOCK(ActionCategory.BLOCK, RiskLevel.MEDIUM),
    BREAK_BLOCK(ActionCategory.BLOCK, RiskLevel.MEDIUM),
    USE_ITEM(ActionCategory.BLOCK, RiskLevel.LOW),
    INTERACT(ActionCategory.BLOCK, RiskLevel.LOW),
    
    // Inventory actions
    CLICK_INVENTORY(ActionCategory.INVENTORY, RiskLevel.LOW),
    DROP_ITEM(ActionCategory.INVENTORY, RiskLevel.LOW),
    SWAP_ITEMS(ActionCategory.INVENTORY, RiskLevel.LOW),
    
    // Chat actions
    SEND_MESSAGE(ActionCategory.CHAT, RiskLevel.LOW),
    COMMAND(ActionCategory.CHAT, RiskLevel.MEDIUM),
    
    // Special actions
    AUTO_CLICK(ActionCategory.SPECIAL, RiskLevel.CRITICAL),
    REACH(ActionCategory.SPECIAL, RiskLevel.CRITICAL),
    SCAFFOLD(ActionCategory.SPECIAL, RiskLevel.CRITICAL),
    KILL_AURA(ActionCategory.SPECIAL, RiskLevel.CRITICAL),
    TIMER_HACK(ActionCategory.SPECIAL, RiskLevel.CRITICAL),
    VELOCITY_BYPASS(ActionCategory.SPECIAL, RiskLevel.CRITICAL)
}

enum class ActionCategory {
    MOVEMENT, COMBAT, BLOCK, INVENTORY, CHAT, SPECIAL
}

enum class RiskLevel(val severity: Int) {
    LOW(1), MEDIUM(5), HIGH(15), CRITICAL(25)
}

data class ActionTarget(
    val type: TargetType,
    val position: Vector3D? = null,
    val entityId: String? = null,
    val blockType: String? = null,
    val distance: Double? = null
)

enum class TargetType {
    BLOCK, ENTITY, AIR, INVENTORY, NONE
}

/**
 * Represents a detected violation with evidence
 */
data class Violation(
    val type: ViolationType,
    val confidence: Double,
    val evidence: List<Evidence>,
    val timestamp: Long,
    val playerId: String
) {
    val age: Long get() = System.currentTimeMillis() - timestamp
}

enum class ViolationType(val severity: Int, val description: String) {
    MOVEMENT_HACK(20, "Invalid movement detected"),
    FLY_HACK(25, "Flying without permission"),
    PHASE_HACK(30, "Phasing through blocks"),
    SPEED_HACK(20, "Movement speed violation"),
    REACH_HACK(25, "Reach distance violation"),
    AUTO_CLICKER(30, "Automated clicking detected"),
    KILL_AURA(35, "Kill aura pattern detected"),
    SCAFFOLD_HACK(25, "Scaffold placement violation"),
    VELOCITY_HACK(20, "Velocity/knockback violation"),
    TIMER_HACK(30, "Game timer manipulation"),
    PACKET_SPOOFING(40, "Packet manipulation detected"),
    BEHAVIOR_HACK(15, "Suspicious behavior pattern"),
    COLLISION_HACK(25, "Collision box violation"),
    INVENTORY_HACK(20, "Inventory manipulation"),
    CHAT_SPAM(10, "Chat spam detected")
}

data class Evidence(
    val type: EvidenceType,
    val value: Any,
    val confidence: Double,
    val description: String
)

enum class EvidenceType {
    POSITION_MISMATCH,
    VELOCITY_ANOMALY,
    TIMING_ANOMALY,
    PATTERN_DETECTION,
    PHYSICS_VIOLATION,
    PACKET_ANOMALY,
    BEHAVIOR_ANOMALY,
    STATISTICAL_ANOMALY
}