package com.csuxac.core.packet

import com.csuxac.core.enforcement.AutomaticActionSystem
import com.csuxac.core.models.PlayerSessionManager
import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.min

/**
 * PacketFlowAnalyzer - Advanced packet analysis and fingerprinting system
 * 
 * Detects:
 * - LiquidBounce packet patterns (Move → Position → Move → Position)
 * - Delayed Flying packets
 * - Timer hack packet compression
 * - Packet spoofing and manipulation
 * - Anomalous packet timing
 * - Client fingerprint mismatches
 */
class PacketFlowAnalyzer(
    private val plugin: org.bukkit.plugin.Plugin,
    private val sessionManager: PlayerSessionManager,
    private val actionSystem: AutomaticActionSystem
) {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Packet flow tracking per player
    private val packetFlows = ConcurrentHashMap<String, PacketFlowTracker>()
    private val clientFingerprints = ConcurrentHashMap<String, ClientFingerprint>()
    private val anomalyScores = ConcurrentHashMap<String, Double>()
    
    // Known cheat client patterns
    private val cheatPatterns = mapOf(
        "liquidbounce" to CheatPattern(
            name = "LiquidBounce",
            patterns = listOf(
                PacketPattern("Move", "Position", "Move", "Position", confidence = 0.9),
                PacketPattern("Flying", "Position", "Move", "Flying", confidence = 0.85),
                PacketPattern("Move", "Move", "Move", "Position", confidence = 0.8)
            ),
            timingAnomalies = listOf(
                TimingAnomaly("Move", "Position", 5, 15, 0.9), // 5-15ms between Move and Position
                TimingAnomaly("Flying", "Move", 10, 25, 0.85)  // 10-25ms between Flying and Move
            ),
            compressionRatios = listOf(0.7, 0.8, 0.9), // Expected compression ratios
            riskLevel = RiskLevel.CRITICAL
        ),
        "wurst" to CheatPattern(
            name = "Wurst",
            patterns = listOf(
                PacketPattern("Move", "Position", "Move", "Position", confidence = 0.8),
                PacketPattern("Attack", "Move", "Attack", "Move", confidence = 0.75)
            ),
            timingAnomalies = listOf(
                TimingAnomaly("Move", "Position", 8, 20, 0.8)
            ),
            compressionRatios = listOf(0.8, 0.9),
            riskLevel = RiskLevel.HIGH
        ),
        "impact" to CheatPattern(
            name = "Impact",
            patterns = listOf(
                PacketPattern("Move", "Move", "Position", "Move", confidence = 0.85),
                PacketPattern("Flying", "Move", "Flying", "Move", confidence = 0.8)
            ),
            timingAnomalies = listOf(
                TimingAnomaly("Move", "Position", 6, 18, 0.85)
            ),
            compressionRatios = listOf(0.75, 0.85, 0.95),
            riskLevel = RiskLevel.HIGH
        )
    )
    
    /**
     * Analyze an incoming packet for anomalies.
     */
    fun analyzePacket(event: com.comphenix.protocol.events.PacketEvent) {
        val player = event.player
        val packetType = event.packetType.toString()
        val timestamp = System.currentTimeMillis()

        scope.launch {
            val tracker = getOrCreateTracker(player.name)
            val violations = mutableListOf<Violation>()
            var confidence = 1.0

            // Record the packet
            val packet = PacketRecord(packetType, timestamp, Vector3D.ZERO, Vector3D.ZERO, player.name)
            tracker.recordPacket(packet)

            // 1. Analyze packet flow pattern
            val flowViolation = analyzePacketFlow(player.name, tracker)
            if (flowViolation != null) {
                violations.add(flowViolation)
                confidence *= 0.6
            }
            
            // 3. Detect packet compression
            val compressionViolation = detectPacketCompression(player.name, tracker)
            if (compressionViolation != null) {
                violations.add(compressionViolation)
                confidence *= 0.8
            }
            
            // 4. Check for client fingerprint mismatch
            val fingerprintViolation = detectFingerprintMismatch(player.name, tracker)
            if (fingerprintViolation != null) {
                violations.add(fingerprintViolation)
                confidence *= 0.5
            }
            
            // 5. Analyze packet size anomalies
            val sizeViolation = detectPacketSizeAnomalies(player.name, tracker)
            if (sizeViolation != null) {
                violations.add(sizeViolation)
                confidence *= 0.9
            }
            
            // 6. Check for sub-tick packet anomalies
            val subTickViolation = detectSubTickPacketAnomalies(player.name, tracker)
            if (subTickViolation != null) {
                violations.add(subTickViolation)
                confidence *= 0.4
            }
            
            // Update anomaly score
            updateAnomalyScore(player.name, violations.size)
            
            if (violations.isNotEmpty()) {
                // Get the player's session and process each violation
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val session = sessionManager.getOrCreateSession(player.name, player.name, player.uniqueId.toString())
                    violations.forEach { violation ->
                        session.addViolation(violation)
                        actionSystem.processViolation(player, violation)
                    }
                })
            }
        }
    }
    
    private fun analyzePacketFlow(playerId: String, tracker: PacketFlowTracker): Violation? {
        val recentPackets = tracker.getRecentPackets(1000) // Last 1 second
        if (recentPackets.size < 4) return null
        
        // Check against known cheat patterns
        for ((clientName, pattern) in cheatPatterns) {
            for (packetPattern in pattern.patterns) {
                if (matchesPattern(recentPackets, packetPattern)) {
                    return Violation(
                        type = ViolationType.PACKET_SPOOFING,
                        confidence = packetPattern.confidence,
                        evidence = listOf(
                            Evidence(
                                type = EvidenceType.PATTERN_DETECTION,
                                value = "Pattern: ${packetPattern.sequence.joinToString(" → ")}",
                                confidence = packetPattern.confidence,
                                description = "Detected $clientName packet pattern"
                            )
                        ),
                        timestamp = System.currentTimeMillis(),
                        playerId = playerId
                    )
                }
            }
        }
        
        return null
    }
    
    private fun detectTimingAnomalies(playerId: String, tracker: PacketFlowTracker): Violation? {
        val recentPackets = tracker.getRecentPackets(500) // Last 500ms
        if (recentPackets.size < 2) return null
        
        // Check for timing anomalies against known patterns
        for ((clientName, pattern) in cheatPatterns) {
            for (timingAnomaly in pattern.timingAnomalies) {
                val anomaly = detectSpecificTimingAnomaly(recentPackets, timingAnomaly)
                if (anomaly) {
                    return Violation(
                        type = ViolationType.TIMER_HACK,
                        confidence = timingAnomaly.confidence,
                        evidence = listOf(
                            Evidence(
                                type = EvidenceType.TIMING_ANOMALY,
                                value = "Timing: ${timingAnomaly.minTime}-${timingAnomaly.maxTime}ms",
                                confidence = timingAnomaly.confidence,
                                description = "Detected $clientName timing anomaly"
                            )
                        ),
                        timestamp = System.currentTimeMillis(),
                        playerId = playerId
                    )
                }
            }
        }
        
        return null
    }
    
    private fun detectPacketCompression(playerId: String, tracker: PacketFlowTracker): Violation? {
        val recentPackets = tracker.getRecentPackets(1000)
        if (recentPackets.size < 10) return null
        
        val compressionRatio = calculateCompressionRatio(recentPackets)
        
        // Check if compression ratio matches known cheat patterns
        for ((clientName, pattern) in cheatPatterns) {
            if (pattern.compressionRatios.any { abs(it - compressionRatio) < 0.1 }) {
                return Violation(
                    type = ViolationType.PACKET_SPOOFING,
                    confidence = 0.8,
                    evidence = listOf(
                        Evidence(
                            type = EvidenceType.PACKET_ANOMALY,
                            value = "Compression ratio: $compressionRatio",
                            confidence = 0.8,
                            description = "Detected $clientName packet compression"
                        )
                    ),
                    timestamp = System.currentTimeMillis(),
                    playerId = playerId
                )
            }
        }
        
        return null
    }
    
    private fun detectFingerprintMismatch(playerId: String, tracker: PacketFlowTracker): Violation? {
        val fingerprint = clientFingerprints[playerId]
        if (fingerprint == null) return null
        
        val currentFingerprint = calculateCurrentFingerprint(tracker)
        val matchScore = compareFingerprints(fingerprint, currentFingerprint)
        
        if (matchScore < 0.7) { // Less than 70% match
            return Violation(
                type = ViolationType.PACKET_SPOOFING,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.PACKET_ANOMALY,
                        value = "Fingerprint match: ${(matchScore * 100).toInt()}%",
                        confidence = 0.9,
                        description = "Client fingerprint mismatch detected"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    private fun detectPacketSizeAnomalies(playerId: String, tracker: PacketFlowTracker): Violation? {
        val recentPackets = tracker.getRecentPackets(100)
        if (recentPackets.isEmpty()) return null
        
        val avgSize = recentPackets.map { it.estimatedSize }.average()
        val variance = recentPackets.map { (it.estimatedSize - avgSize).pow(2.0) }.average()
        val stdDev = sqrt(variance)
        
        // Check for unusually consistent packet sizes (bot-like behavior)
        if (stdDev < 0.1 && recentPackets.size > 20) {
            return Violation(
                type = ViolationType.BEHAVIOR_HACK,
                confidence = 0.85,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.STATISTICAL_ANOMALY,
                        value = "Packet size std dev: $stdDev",
                        confidence = 0.85,
                        description = "Unusually consistent packet sizes detected"
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    private fun detectSubTickPacketAnomalies(playerId: String, tracker: PacketFlowTracker): Violation? {
        val recentPackets = tracker.getRecentPackets(1000) // Get packets from the last second
        val packetCount = recentPackets.size

        // A legitimate client sends about 20 packets per second.
        // We allow a buffer for network jitter and other anomalies.
        val threshold = 30

        if (packetCount > threshold) {
            return Violation(
                type = ViolationType.TIMER_HACK,
                confidence = 0.9,
                evidence = listOf(
                    Evidence(
                        type = EvidenceType.TIMING_ANOMALY,
                        value = "Packet count: $packetCount/s",
                        confidence = 0.9,
                        description = "Exceeded packet threshold of $threshold/s."
                    )
                ),
                timestamp = System.currentTimeMillis(),
                playerId = playerId
            )
        }
        
        return null
    }
    
    private fun matchesPattern(packets: List<PacketRecord>, pattern: PacketPattern): Boolean {
        if (packets.size < pattern.sequence.size) return false
        
        // Check if the last N packets match the pattern
        val lastPackets = packets.takeLast(pattern.sequence.size)
        return lastPackets.map { it.type } == pattern.sequence
    }
    
    private fun detectSpecificTimingAnomaly(packets: List<PacketRecord>, anomaly: TimingAnomaly): Boolean {
        val relevantPackets = packets.filter { it.type in listOf(anomaly.packet1, anomaly.packet2) }
        
        for (i in 0 until relevantPackets.size - 1) {
            if (relevantPackets[i].type == anomaly.packet1 && relevantPackets[i + 1].type == anomaly.packet2) {
                val timeDelta = relevantPackets[i + 1].timestamp - relevantPackets[i].timestamp
                if (timeDelta in anomaly.minTime..anomaly.maxTime) {
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun calculateCompressionRatio(packets: List<PacketRecord>): Double {
        if (packets.size < 2) return 1.0
        
        val totalTime = packets.last().timestamp - packets.first().timestamp
        val expectedTime = packets.size * 50.0 // 50ms per packet at 20 TPS
        return totalTime / expectedTime
    }
    
    private fun calculateCurrentFingerprint(tracker: PacketFlowTracker): ClientFingerprint {
        val recentPackets = tracker.getRecentPackets(1000)
        
        return ClientFingerprint(
            avgPacketSize = recentPackets.map { it.estimatedSize }.average(),
            packetTimingVariance = calculateTimingVariance(recentPackets),
            compressionRatio = calculateCompressionRatio(recentPackets),
            commonPatterns = extractCommonPatterns(recentPackets)
        )
    }
    
    private fun compareFingerprints(expected: ClientFingerprint, actual: ClientFingerprint): Double {
        val sizeScore = 1.0 - abs(expected.avgPacketSize - actual.avgPacketSize) / expected.avgPacketSize
        val timingScore = 1.0 - abs(expected.packetTimingVariance - actual.packetTimingVariance) / expected.packetTimingVariance
        val compressionScore = 1.0 - abs(expected.compressionRatio - actual.compressionRatio) / expected.compressionRatio
        
        return (sizeScore + timingScore + compressionScore) / 3.0
    }
    
    private fun calculateTimingVariance(packets: List<PacketRecord>): Double {
        if (packets.size < 2) return 0.0
        
        val intervals = packets.zipWithNext().map { (prev, next) ->
            next.timestamp - prev.timestamp
        }
        
        val avgInterval = intervals.average()
        return intervals.map { (it - avgInterval).pow(2.0) }.average()
    }
    
    private fun extractCommonPatterns(packets: List<PacketRecord>): List<String> {
        if (packets.size < 3) return emptyList()
        
        val patterns = mutableListOf<String>()
        for (i in 0 until packets.size - 2) {
            val pattern = "${packets[i].type} → ${packets[i + 1].type} → ${packets[i + 2].type}"
            patterns.add(pattern)
        }
        
        return patterns.groupBy { it }.mapValues { it.value.size }.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }
    
    private fun determineFlowPattern(tracker: PacketFlowTracker): PacketFlowPattern {
        val recentPackets = tracker.getRecentPackets(100)
        
        return when {
            recentPackets.size < 4 -> PacketFlowPattern.NORMAL
            hasCompressedPattern(recentPackets) -> PacketFlowPattern.COMPRESSED
            hasDelayedPattern(recentPackets) -> PacketFlowPattern.DELAYED
            hasSpoofedPattern(recentPackets) -> PacketFlowPattern.SPOOFED
            hasAnomalousPattern(recentPackets) -> PacketFlowPattern.ANOMALOUS
            else -> PacketFlowPattern.NORMAL
        }
    }
    
    private fun hasCompressedPattern(packets: List<PacketRecord>): Boolean {
        val compressionRatio = calculateCompressionRatio(packets)
        return compressionRatio < 0.8
    }
    
    private fun hasDelayedPattern(packets: List<PacketRecord>): Boolean {
        val intervals = packets.zipWithNext().map { (prev, next) ->
            next.timestamp - prev.timestamp
        }
        return intervals.any { it > 100 } // More than 100ms between packets
    }
    
    private fun hasSpoofedPattern(packets: List<PacketRecord>): Boolean {
        // Check for impossible packet sequences
        return packets.zipWithNext().any { (prev, next) ->
            prev.type == "Position" && next.type == "Move" && 
            (next.timestamp - prev.timestamp) < 10
        }
    }
    
    private fun hasAnomalousPattern(packets: List<PacketRecord>): Boolean {
        val anomalyScore = anomalyScores[packets.firstOrNull()?.playerId ?: ""] ?: 0.0
        return anomalyScore > 0.7
    }
    
    private fun calculateFingerprintMatch(playerId: String, tracker: PacketFlowTracker): Double {
        val fingerprint = clientFingerprints[playerId] ?: return 1.0
        val current = calculateCurrentFingerprint(tracker)
        return compareFingerprints(fingerprint, current)
    }
    
    private fun updateAnomalyScore(playerId: String, violationCount: Int) {
        val currentScore = anomalyScores.getOrDefault(playerId, 0.0)
        val newScore = currentScore + (violationCount * 0.1)
        anomalyScores[playerId] = min(1.0, newScore)
    }
    
    private fun getOrCreateTracker(playerId: String): PacketFlowTracker {
        return packetFlows.getOrPut(playerId) { PacketFlowTracker(playerId) }
    }
    
    // Data classes for pattern matching
    private data class CheatPattern(
        val name: String,
        val patterns: List<PacketPattern>,
        val timingAnomalies: List<TimingAnomaly>,
        val compressionRatios: List<Double>,
        val riskLevel: RiskLevel
    )
    
    private data class PacketPattern(
        val sequence: List<String>,
        val confidence: Double
    ) {
        constructor(vararg types: String, confidence: Double) : this(types.toList(), confidence)
    }
    
    private data class TimingAnomaly(
        val packet1: String,
        val packet2: String,
        val minTime: Long,
        val maxTime: Long,
        val confidence: Double
    )
    
    private data class ClientFingerprint(
        val avgPacketSize: Double,
        val packetTimingVariance: Double,
        val compressionRatio: Double,
        val commonPatterns: List<String>
    )
    
    private data class PacketRecord(
        val type: String,
        val timestamp: Long,
        val from: Vector3D,
        val to: Vector3D,
        val playerId: String = "UNKNOWN"
    ) {
        val estimatedSize: Double get() = when (type) {
            "Move" -> 25.0
            "Position" -> 15.0
            "Flying" -> 10.0
            else -> 20.0
        }
    }
    
    private data class PacketFlowTracker(
        val playerId: String,
        private val packets: MutableList<PacketRecord> = mutableListOf()
    ) {
        fun recordPacket(packet: PacketRecord) {
            packets.add(packet)
            if (packets.size > 1000) {
                packets.removeAt(0)
            }
        }
        
        fun getRecentPackets(withinMs: Long): List<PacketRecord> {
            val cutoff = System.currentTimeMillis() - withinMs
            return packets.filter { it.timestamp > cutoff }
        }
    }
}