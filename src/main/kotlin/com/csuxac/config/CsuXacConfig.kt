package com.csuxac.config

import java.io.File

data class CsuXacConfig(
    val security: SecurityConfig = SecurityConfig(),
    val cluster: ClusterConfig = ClusterConfig(),
    val monitoring: MonitoringConfig = MonitoringConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    val ai: AIConfig = AIConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val client: ClientConfig = ClientConfig()
) {
    companion object {
        fun load(configPath: String? = null): CsuXacConfig {
            // For now, just return default configuration
            // In a real implementation, this would load from YAML/JSON
            return CsuXacConfig()
        }
    }
}

data class SecurityConfig(
    val strictMode: Boolean = true,
    val maxThreatLevel: Int = 100,
    val autoResponse: Boolean = true,
    val quarantineEnabled: Boolean = true
)

data class ClusterConfig(
    val enabled: Boolean = false,
    val redisHost: String = "localhost",
    val redisPort: Int = 6379,
    val redisPassword: String? = null,
    val nodeId: String = "node-1"
)

data class MonitoringConfig(
    val enabled: Boolean = true,
    val metricsEnabled: Boolean = true,
    val alertingEnabled: Boolean = true,
    val dashboardPort: Int = 8080
)

data class PerformanceConfig(
    val maxThreads: Int = Runtime.getRuntime().availableProcessors(),
    val objectPoolSize: Int = 1000,
    val offHeapEnabled: Boolean = false,
    val gcOptimization: Boolean = true
)

data class AIConfig(
    val enabled: Boolean = true,
    val modelPath: String = "models/",
    val trainingEnabled: Boolean = false,
    val confidenceThreshold: Double = 0.8
)

data class NetworkConfig(
    val maxConnections: Int = 10000,
    val connectionTimeout: Long = 30000,
    val rateLimitEnabled: Boolean = true,
    val maxPacketsPerSecond: Int = 1000
)

data class ClientConfig(
    val agentEnabled: Boolean = true,
    val agentUpdateInterval: Long = 60000,
    val memoryScanEnabled: Boolean = true,
    val threadMonitoringEnabled: Boolean = true
)