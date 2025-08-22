package com.csuxac

import com.csuxac.config.CsuXacConfig
import com.csuxac.core.SecurityEngine
import com.csuxac.core.cluster.ClusterManager
import com.csuxac.core.monitoring.MonitoringDashboard
import com.csuxac.util.logging.defaultLogger

/**
 * CsuXac Core Enforcement Directive - Main Application
 */
class CsuXacApplication {
    private val logger = defaultLogger()
    private val config = CsuXacConfig.load()
    
    private val securityEngine = SecurityEngine(config.security)
    private val clusterManager = ClusterManager(config.cluster)
    private val monitoringDashboard = MonitoringDashboard(config.monitoring)
    
    suspend fun start() {
        logger.info { "🚀 Starting CsuXac Core Enforcement Directive..." }
        
        try {
            securityEngine.start()
            clusterManager.start()
            monitoringDashboard.start()
            
            logger.info { "✅ CsuXac Core fully operational" }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to start CsuXac Core" }
            throw e
        }
    }
    
    suspend fun stop() {
        logger.info { "🛑 Shutting down CsuXac Core..." }
        
        securityEngine.stop()
        clusterManager.stop()
        monitoringDashboard.stop()
        
        logger.info { "✅ CsuXac Core shutdown complete" }
    }
}

suspend fun main() {
    val app = CsuXacApplication()
    
    try {
        app.start()
        
        // Keep running
        while (true) {
            kotlinx.coroutines.delay(1000)
        }
    } catch (e: Exception) {
        defaultLogger().error(e) { "Fatal error in CsuXac Core" }
        app.stop()
    }
}