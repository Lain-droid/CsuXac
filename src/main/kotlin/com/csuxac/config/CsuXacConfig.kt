package com.csuxac.config

/**
 * Main configuration for CsuXac Core
 */
data class CsuXacConfig(
    val security: SecurityConfig = SecurityConfig(),
    val cluster: ClusterConfig = ClusterConfig(),
    val monitoring: MonitoringConfig = MonitoringConfig()
) {
    companion object {
        fun load(): CsuXacConfig {
            return CsuXacConfig()
        }
    }
}

data class ClusterConfig(
    val enabled: Boolean = false,
    val nodeId: String = "csuxac-node-1",
    val nodes: List<String> = emptyList()
)

data class MonitoringConfig(
    val enabled: Boolean = true,
    val port: Int = 8080,
    val metricsEnabled: Boolean = true
)