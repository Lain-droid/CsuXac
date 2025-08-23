package com.csuxac.core.models

/**
 * Packet record for analysis
 */
data class PacketRecord(
    val id: String,
    val type: String,
    val estimatedSize: Int,
    val processingTime: Long,
    val timestamp: Long,
    val playerId: String,
    val data: Map<String, Any> = emptyMap()
)