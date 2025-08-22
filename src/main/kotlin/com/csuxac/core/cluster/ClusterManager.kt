package com.csuxac.core.cluster

import com.csuxac.config.ClusterConfig
import com.csuxac.util.logging.defaultLogger

/**
 * Simple cluster management for CsuXac Core
 */
class ClusterManager(
    private val config: ClusterConfig
) {
    private val logger = defaultLogger()
    private var isRunning = false
    
    fun start() {
        if (isRunning) return
        
        logger.info { "Starting Cluster Manager..." }
        isRunning = true
        logger.info { "Cluster Manager started" }
    }
    
    fun stop() {
        if (!isRunning) return
        
        logger.info { "Stopping Cluster Manager..." }
        isRunning = false
        logger.info { "Cluster Manager stopped" }
    }
    
    fun isRunning(): Boolean = isRunning
}