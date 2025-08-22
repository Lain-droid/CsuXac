package com.csuxac

import com.csuxac.config.CsuXacConfig
import com.csuxac.core.SecurityEngine
import com.csuxac.core.cluster.ClusterManager
import com.csuxac.core.monitoring.MonitoringDashboard
import com.csuxac.util.logging.defaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

class CsuXacApplication {
    private val logger = defaultLogger()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private lateinit var config: CsuXacConfig
    private lateinit var securityEngine: SecurityEngine
    private lateinit var clusterManager: ClusterManager
    private lateinit var monitoringDashboard: MonitoringDashboard
    
    suspend fun start() {
        try {
            logger.info { "Starting CsuXac Ultimate Minecraft Anti-Cheat Infrastructure..." }
            
            // Initialize configuration
            config = CsuXacConfig.load()
            logger.info { "Configuration loaded successfully" }
            
            // Initialize core components
            securityEngine = SecurityEngine(config.security)
            clusterManager = ClusterManager(config.cluster)
            monitoringDashboard = MonitoringDashboard(config.monitoring)
            
            // Start components
            securityEngine.start()
            clusterManager.start()
            monitoringDashboard.start()
            
            logger.info { "CsuXac started successfully" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to start CsuXac" }
            throw e
        }
    }
    
    suspend fun shutdown() {
        try {
            logger.info { "Shutting down CsuXac..." }
            
            monitoringDashboard.stop()
            clusterManager.stop()
            securityEngine.stop()
            
            logger.info { "CsuXac shut down successfully" }
            
        } catch (e: Exception) {
            logger.error(e) { "Error during shutdown" }
        }
    }
}

fun main(args: Array<String>) = runBlocking {
    val app = CsuXacApplication()
    
    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            app.shutdown()
        }
    })
    
    try {
        app.start()
        
        // Keep the application running
        while (true) {
            kotlinx.coroutines.delay(1000)
        }
        
    } catch (e: Exception) {
        defaultLogger().error(e) { "Application failed" }
        exitProcess(1)
    }
}