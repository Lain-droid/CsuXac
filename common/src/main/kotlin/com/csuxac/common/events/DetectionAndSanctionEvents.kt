package com.csuxac.common.events

import kotlinx.serialization.Serializable

@Serializable
data class DetectionResultEvent(
    override val playerId: String,
    override val timestamp: Long,
    val cheatType: String,
    val confidence: Double
) : Event

@Serializable
enum class SanctionType { KICK, BAN, WARN, SHADOW }

@Serializable
data class SanctionEvent(
    override val playerId: String,
    override val timestamp: Long,
    val sanctionType: SanctionType,
    val reason: String,
    val confidence: Double
) : Event