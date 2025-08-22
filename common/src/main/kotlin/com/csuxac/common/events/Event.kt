package com.csuxac.common.events

import kotlinx.serialization.Serializable

@Serializable
sealed interface Event {
    val playerId: String
    val timestamp: Long
}

@Serializable
data class MovementEvent(
    override val playerId: String,
    override val timestamp: Long,
    val x: Double,
    val y: Double,
    val z: Double,
    val onGround: Boolean
) : Event

@Serializable
data class ClickEvent(
    override val playerId: String,
    override val timestamp: Long,
    val cps: Double
) : Event

// more event types can be added similarly