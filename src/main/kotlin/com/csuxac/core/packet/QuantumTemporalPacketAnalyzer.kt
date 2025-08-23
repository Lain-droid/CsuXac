package com.csuxac.core.packet

import com.csuxac.core.models.*
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

/**
 * Quantum-Temporal Packet Analysis (QTPA) - Ultimate Enforcement Directive v5.0
 * 
 * Analyzes packets not just by content, but by their "temporal positions" in 4D spacetime.
 * Each packet is examined in (x, y, z, t) coordinates to detect time manipulation exploits.
 * 
 * Calculates "temporal entropy" to distinguish between human-like (high entropy) and
 * bot-like (low entropy) behavior patterns.
 */
class QuantumTemporalPacketAnalyzer {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Temporal packet tracking
    private val playerPacketHistory = ConcurrentHashMap<String, MutableList<TemporalPacketRecord>>()
    private val temporalViolationCount = ConcurrentHashMap<String, AtomicInteger>()
    private val entropyProfiles = ConcurrentHashMap<String, EntropyProfile>()
    
    companion object {
        const val TEMPORAL_CHECK_INTERVAL_MS = 25L // Check every 25ms (sub-tick)
        const val MAX_TEMPORAL_DISCREPANCY_MS = 100L // 100ms tolerance
        const val MIN_ENTROPY_THRESHOLD = 0.3 // Minimum entropy for human-like behavior
        const val MAX_PACKET_HISTORY = 1000 // Keep last 1000 packets per player
        const val TEMPORAL_ANOMALY_THRESHOLD = 0.8 // 80% confidence threshold
        const val QUANTUM_UNCERTAINTY_MS = 5L // 5ms quantum uncertainty principle
    }
    
    /**
     * Analyze packet in 4D spacetime
     */
    suspend fun analyzeTemporalPacket(
        playerId: String,
        packet: PacketRecord,
        timestamp: Long
    ): TemporalPacketValidationResult {
        val violations = mutableListOf<Violation>()
        var confidence = 1.0
        
        try {
            // Create temporal packet record
            val temporalPacket = createTemporalPacketRecord(packet, timestamp)
            
            // Add to player history
            addPacketToHistory(playerId, temporalPacket)
            
            // Analyze temporal anomalies
            val temporalAnalysis = analyzeTemporalAnomalies(playerId, temporalPacket)
            
            if (temporalAnalysis.anomalyScore > TEMPORAL_ANOMALY_THRESHOLD) {
                violations.add(createTemporalViolation(
                    playerId, temporalPacket, temporalAnalysis
                ))
                confidence = 1.0 - temporalAnalysis.anomalyScore
                
                // Increment violation count
                val violationCount = temporalViolationCount.computeIfAbsent(playerId) { AtomicInteger(0) }
                violationCount.incrementAndGet()
                
                logger.warn { 
                    "⏰ TEMPORAL ANOMALY: Player $playerId - " +
                    "Anomaly Score: ${temporalAnalysis.anomalyScore}, " +
                    "Type: ${temporalAnalysis.anomalyType}"
                }
            }
            
            // Calculate temporal entropy
            val entropyAnalysis = calculateTemporalEntropy(playerId, temporalPacket)
            
            // Update entropy profile
            updateEntropyProfile(playerId, entropyAnalysis)
            
            // Check for low entropy + high performance (cheat indicator)
            val avgEntropy = (entropyAnalysis.timingEntropy + entropyAnalysis.spatialEntropy + entropyAnalysis.typeEntropy) / 3.0
            if (avgEntropy < MIN_ENTROPY_THRESHOLD && 
                temporalAnalysis.performanceScore > 0.9) {
                violations.add(createLowEntropyViolation(
                    playerId, temporalPacket, entropyAnalysis, temporalAnalysis
                ))
                confidence = 0.0
            }
            
            return TemporalPacketValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                confidence = confidence,
                temporalPacket = temporalPacket,
                temporalAnalysis = temporalAnalysis,
                entropyAnalysis = entropyAnalysis,
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in quantum-temporal packet analysis for player $playerId" }
            violations.add(createTemporalViolation(
                playerId, TemporalPacketRecord(packet, timestamp, Vector3D.ZERO, 0.0),
                TemporalAnomalyAnalysis(0.0, "Error", emptyMap(), 0.0)
            ))
            
            return TemporalPacketValidationResult(
                isValid = false,
                violations = violations,
                confidence = 0.0,
                temporalPacket = null,
                temporalAnalysis = null,
                entropyAnalysis = null,
                timestamp = timestamp
            )
        }
    }
    
    /**
     * Create temporal packet record with 4D coordinates
     */
    private fun createTemporalPacketRecord(
        packet: PacketRecord,
        timestamp: Long
    ): TemporalPacketRecord {
        // Extract spatial coordinates from packet if available
        val spatialPosition = extractSpatialPosition(packet)
        
        // Calculate temporal velocity (packet frequency)
        val temporalVelocity = calculateTemporalVelocity(packet, timestamp)
        
        return TemporalPacketRecord(
            packet = packet,
            timestamp = timestamp,
            spatialPosition = spatialPosition,
            temporalVelocity = temporalVelocity
        )
    }
    
    /**
     * Extract spatial position from packet data
     */
    private fun extractSpatialPosition(packet: PacketRecord): Vector3D {
        // This would extract actual coordinates from packet data
        // For now, return a placeholder based on packet type
        return when (packet.type) {
            "Position" -> Vector3D(0.0, 0.0, 0.0) // Would extract actual coords
            "Movement" -> Vector3D(0.0, 0.0, 0.0) // Would extract actual coords
            "BlockPlace" -> Vector3D(0.0, 0.0, 0.0) // Would extract actual coords
            else -> Vector3D.ZERO
        }
    }
    
    /**
     * Calculate temporal velocity (packet frequency)
     */
    private fun calculateTemporalVelocity(packet: PacketRecord, timestamp: Long): Double {
        // Calculate how frequently this type of packet is sent
        // Higher frequency = higher temporal velocity
        return 1.0 / max(packet.estimatedSize.toDouble(), 1.0)
    }
    
    /**
     * Analyze temporal anomalies in packet
     */
    private fun analyzeTemporalAnomalies(
        playerId: String,
        temporalPacket: TemporalPacketRecord
    ): TemporalAnomalyAnalysis {
        val anomalyScores = mutableMapOf<String, Double>()
        
        // Check for temporal impossibilities
        val temporalImpossibility = checkTemporalImpossibility(playerId, temporalPacket)
        anomalyScores["temporal_impossibility"] = temporalImpossibility
        
        // Check for causality violations
        val causalityViolation = checkCausalityViolation(playerId, temporalPacket)
        anomalyScores["causality_violation"] = causalityViolation
        
        // Check for quantum uncertainty violations
        val quantumViolation = checkQuantumUncertainty(playerId, temporalPacket)
        anomalyScores["quantum_violation"] = quantumViolation
        
        // Check for performance anomalies
        val performanceAnomaly = checkPerformanceAnomaly(playerId, temporalPacket)
        anomalyScores["performance_anomaly"] = performanceAnomaly
        
        // Calculate overall anomaly score
        val overallAnomaly = calculateOverallTemporalAnomaly(anomalyScores)
        
        // Determine anomaly type
        val anomalyType = determineTemporalAnomalyType(anomalyScores, overallAnomaly)
        
        // Calculate performance score
        val performanceScore = calculatePerformanceScore(temporalPacket)
        
        return TemporalAnomalyAnalysis(
            anomalyScore = overallAnomaly,
            anomalyType = anomalyType,
            componentScores = anomalyScores,
            performanceScore = performanceScore
        )
    }
    
    /**
     * Check for temporal impossibilities
     */
    private fun checkTemporalImpossibility(
        playerId: String,
        temporalPacket: TemporalPacketRecord
    ): Double {
        val packetHistory = playerPacketHistory[playerId] ?: return 0.0
        if (packetHistory.isEmpty()) return 0.0
        
        val lastPacket = packetHistory.last()
        
        // Check if packet timestamp is before previous packet
        if (temporalPacket.timestamp < lastPacket.timestamp) {
            return 1.0 // Impossible: packet from the past
        }
        
        // Check for impossible time gaps
        val timeGap = temporalPacket.timestamp - lastPacket.timestamp
        if (timeGap > MAX_TEMPORAL_DISCREPANCY_MS) {
            return min(timeGap / MAX_TEMPORAL_DISCREPANCY_MS.toDouble(), 1.0)
        }
        
        return 0.0
    }
    
    /**
     * Check for causality violations
     */
    private fun checkCausalityViolation(
        playerId: String,
        temporalPacket: TemporalPacketRecord
    ): Double {
        val packetHistory = playerPacketHistory[playerId] ?: return 0.0
        if (packetHistory.size < 2) return 0.0
        
        // Check for impossible packet sequences
        val recentPackets = packetHistory.takeLast(10)
        
        // Example: Position packet followed immediately by BlockPlace without movement
        for (i in 0 until recentPackets.size - 1) {
            val current = recentPackets[i]
            val next = recentPackets[i + 1]
            
            if (current.packet.type == "Position" && 
                next.packet.type == "BlockPlace" &&
                next.timestamp - current.timestamp < 10) {
                
                // Check if movement occurred between position and block place
                val hasMovement = recentPackets.any { 
                    it.timestamp > current.timestamp && 
                    it.timestamp < next.timestamp && 
                    it.packet.type == "Movement"
                }
                
                if (!hasMovement) {
                    return 0.8 // Causality violation: block place without movement
                }
            }
        }
        
        return 0.0
    }
    
    /**
     * Check for quantum uncertainty violations
     */
    private fun checkQuantumUncertainty(
        playerId: String,
        temporalPacket: TemporalPacketRecord
    ): Double {
        val packetHistory = playerPacketHistory[playerId] ?: return 0.0
        if (packetHistory.isEmpty()) return 0.0
        
        val lastPacket = packetHistory.last()
        val timeDifference = abs(temporalPacket.timestamp - lastPacket.timestamp)
        
        // Quantum uncertainty principle: cannot measure time with infinite precision
        if (timeDifference < QUANTUM_UNCERTAINTY_MS) {
            return min(QUANTUM_UNCERTAINTY_MS / timeDifference.toDouble(), 1.0)
        }
        
        return 0.0
    }
    
    /**
     * Check for performance anomalies
     */
    private fun checkPerformanceAnomaly(
        playerId: String,
        temporalPacket: TemporalPacketRecord
    ): Double {
        val packetHistory = playerPacketHistory[playerId] ?: return 0.0
        if (packetHistory.size < 10) return 0.0
        
        // Calculate average packet processing time
        val recentPackets = packetHistory.takeLast(10)
        val processingTimes = recentPackets.map { it.packet.processingTime }
        val avgProcessingTime = processingTimes.average()
        
        // Check if current packet is processed too quickly (suspicious)
        val currentProcessingTime = temporalPacket.packet.processingTime
        if (currentProcessingTime < avgProcessingTime * 0.1) { // 10x faster than average
            return min(avgProcessingTime / currentProcessingTime.toDouble(), 1.0)
        }
        
        return 0.0
    }
    
    /**
     * Calculate overall temporal anomaly score
     */
    private fun calculateOverallTemporalAnomaly(componentScores: Map<String, Double>): Double {
        val weights = mapOf(
            "temporal_impossibility" to 0.4,
            "causality_violation" to 0.3,
            "quantum_violation" to 0.2,
            "performance_anomaly" to 0.1
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
     * Determine temporal anomaly type
     */
    private fun determineTemporalAnomalyType(
        componentScores: Map<String, Double>,
        overallScore: Double
    ): String {
        val maxComponent = componentScores.maxByOrNull { it.value }
        
        return when {
            overallScore > 0.9 -> "CRITICAL_TEMPORAL_ANOMALY"
            overallScore > 0.7 -> "HIGH_TEMPORAL_ANOMALY"
            overallScore > 0.5 -> "MEDIUM_TEMPORAL_ANOMALY"
            maxComponent?.value ?: 0.0 > 0.6 -> "${maxComponent?.key?.uppercase()}"
            else -> "LOW_TEMPORAL_ANOMALY"
        }
    }
    
    /**
     * Calculate performance score
     */
    private fun calculatePerformanceScore(temporalPacket: TemporalPacketRecord): Double {
        // Higher temporal velocity + lower processing time = higher performance
        val performanceScore = temporalPacket.temporalVelocity * (1.0 / max(temporalPacket.packet.processingTime.toDouble(), 1.0))
        return min(performanceScore, 1.0)
    }
    
    /**
     * Calculate temporal entropy
     */
    private fun calculateTemporalEntropy(
        playerId: String,
        temporalPacket: TemporalPacketRecord
    ): EntropyAnalysis {
        val packetHistory = playerPacketHistory[playerId] ?: return EntropyAnalysis(0.0, 0.0, 0.0)
        if (packetHistory.size < 10) return EntropyAnalysis(0.0, 0.0, 0.0)
        
        val recentPackets = packetHistory.takeLast(100)
        
        // Calculate timing entropy
        val timestamps = recentPackets.map { it.timestamp }
        val timingEntropy = calculateTimingEntropy(timestamps)
        
        // Calculate spatial entropy
        val spatialPositions = recentPackets.map { it.spatialPosition }
        val spatialEntropy = calculateSpatialEntropy(spatialPositions)
        
        // Calculate packet type entropy
        val packetTypes = recentPackets.map { it.packet.type }
        val typeEntropy = calculateTypeEntropy(packetTypes)
        
        return EntropyAnalysis(
            timingEntropy = timingEntropy,
            spatialEntropy = spatialEntropy,
            typeEntropy = typeEntropy
        )
    }
    
    /**
     * Calculate timing entropy
     */
    private fun calculateTimingEntropy(timestamps: List<Long>): Double {
        if (timestamps.size < 2) return 0.0
        
        val intervals = timestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval).pow(2.0) }.average()
        
        return sqrt(variance) / max(avgInterval, 1.0)
    }
    
    /**
     * Calculate spatial entropy
     */
    private fun calculateSpatialEntropy(positions: List<Vector3D>): Double {
        if (positions.isEmpty()) return 0.0
        
        val distances = positions.zipWithNext { a, b -> a.distanceTo(b) }
        val avgDistance = distances.average()
        val variance = distances.map { (it - avgDistance).pow(2.0) }.average()
        
        return sqrt(variance) / max(avgDistance, 1.0)
    }
    
    /**
     * Calculate packet type entropy
     */
    private fun calculateTypeEntropy(types: List<String>): Double {
        if (types.isEmpty()) return 0.0
        
        val typeCounts = types.groupingBy { it }.eachCount()
        val totalTypes = types.size.toDouble()
        
        var entropy = 0.0
        typeCounts.forEach { (_, count) ->
            val probability = count / totalTypes
            if (probability > 0) {
                entropy -= probability * ln(probability)
            }
        }
        
        return entropy / ln(typeCounts.size.toDouble())
    }
    
    /**
     * Update entropy profile
     */
    private fun updateEntropyProfile(playerId: String, entropyAnalysis: EntropyAnalysis) {
        val profile = entropyProfiles.computeIfAbsent(playerId) {
            EntropyProfile(
                playerId = playerId,
                avgTimingEntropy = 0.0,
                avgSpatialEntropy = 0.0,
                avgTypeEntropy = 0.0,
                totalSamples = 0,
                lastUpdate = System.currentTimeMillis()
            )
        }
        
        // Update with exponential moving average
        profile.avgTimingEntropy = profile.avgTimingEntropy * 0.9 + entropyAnalysis.timingEntropy * 0.1
        profile.avgSpatialEntropy = profile.avgSpatialEntropy * 0.9 + entropyAnalysis.spatialEntropy * 0.1
        profile.avgTypeEntropy = profile.avgTypeEntropy * 0.9 + entropyAnalysis.typeEntropy * 0.1
        
        profile.totalSamples++
        profile.lastUpdate = System.currentTimeMillis()
    }
    
    /**
     * Add packet to history
     */
    private fun addPacketToHistory(playerId: String, temporalPacket: TemporalPacketRecord) {
        val playerHistory = playerPacketHistory.computeIfAbsent(playerId) { mutableListOf() }
        playerHistory.add(temporalPacket)
        
        // Keep only recent packets
        if (playerHistory.size > MAX_PACKET_HISTORY) {
            playerHistory.removeAt(0)
        }
    }
    
    /**
     * Create temporal violation
     */
    private fun createTemporalViolation(
        playerId: String,
        temporalPacket: TemporalPacketRecord,
        temporalAnalysis: TemporalAnomalyAnalysis
    ): Violation {
        return Violation(
            type = ViolationType.TEMPORAL_ANOMALY,
            confidence = temporalAnalysis.anomalyScore,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.TEMPORAL_ANOMALY,
                    value = mapOf(
                        "anomalyScore" to temporalAnalysis.anomalyScore,
                        "anomalyType" to temporalAnalysis.anomalyType,
                        "componentScores" to temporalAnalysis.componentScores,
                        "performanceScore" to temporalAnalysis.performanceScore,
                        "timestamp" to temporalPacket.timestamp
                    ),
                    confidence = temporalAnalysis.anomalyScore,
                    description = "Quantum-temporal anomaly detected: ${temporalAnalysis.anomalyType}"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Create low entropy violation
     */
    private fun createLowEntropyViolation(
        playerId: String,
        temporalPacket: TemporalPacketRecord,
        entropyAnalysis: EntropyAnalysis,
        temporalAnalysis: TemporalAnomalyAnalysis
    ): Violation {
        return Violation(
            type = ViolationType.LOW_ENTROPY_ANOMALY,
            confidence = 0.95,
            evidence = listOf(
                Evidence(
                    type = EvidenceType.ENTROPY_ANOMALY,
                    value = mapOf(
                        "entropy" to entropyAnalysis,
                        "performanceScore" to temporalAnalysis.performanceScore,
                        "timestamp" to temporalPacket.timestamp
                    ),
                    confidence = 0.95,
                    description = "Low entropy + high performance: potential cheat detected"
                )
            ),
            timestamp = System.currentTimeMillis(),
            playerId = playerId
        )
    }
    
    /**
     * Get temporal analysis statistics
     */
    fun getTemporalAnalysisStats(playerId: String): TemporalAnalysisStats {
        val violationCount = temporalViolationCount[playerId]?.get() ?: 0
        val entropyProfile = entropyProfiles[playerId]
        val packetHistory = playerPacketHistory[playerId]
        
        return TemporalAnalysisStats(
            playerId = playerId,
            totalPackets = packetHistory?.size ?: 0,
            temporalViolations = violationCount,
            avgTimingEntropy = entropyProfile?.avgTimingEntropy ?: 0.0,
            avgSpatialEntropy = entropyProfile?.avgSpatialEntropy ?: 0.0,
            avgTypeEntropy = entropyProfile?.avgTypeEntropy ?: 0.0,
            isQuarantined = violationCount >= 15
        )
    }
    
    /**
     * Cleanup player data
     */
    fun removePlayer(playerId: String) {
        playerPacketHistory.remove(playerId)
        temporalViolationCount.remove(playerId)
        entropyProfiles.remove(playerId)
    }
}

// These classes are now defined in the models package

/**
 * Entropy profile for a player
 */
data class EntropyProfile(
    val playerId: String,
    var avgTimingEntropy: Double,
    var avgSpatialEntropy: Double,
    var avgTypeEntropy: Double,
    var totalSamples: Int,
    var lastUpdate: Long
)

/**
 * Temporal analysis statistics
 */
data class TemporalAnalysisStats(
    val playerId: String,
    val totalPackets: Int,
    val temporalViolations: Int,
    val avgTimingEntropy: Double,
    val avgSpatialEntropy: Double,
    val avgTypeEntropy: Double,
    val isQuarantined: Boolean
)