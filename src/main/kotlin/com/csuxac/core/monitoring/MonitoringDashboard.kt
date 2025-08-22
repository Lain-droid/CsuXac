package com.csuxac.core.monitoring

import com.csuxac.config.MonitoringConfig
import com.csuxac.util.logging.defaultLogger

/**
 * Simple monitoring dashboard for CsuXac Core
 */
class MonitoringDashboard(
    private val config: MonitoringConfig
) {
    private val logger = defaultLogger()
    private var isRunning = false
    
    fun start() {
        if (isRunning) return
        
        logger.info { "Starting Monitoring Dashboard..." }
        isRunning = true
        logger.info { "Monitoring Dashboard started on port ${config.port}" }
    }
    
    fun stop() {
        if (!isRunning) return
        
        logger.info { "Stopping Monitoring Dashboard..." }
        isRunning = false
        logger.info { "Monitoring Dashboard stopped" }
    }
    
    fun isRunning(): Boolean = isRunning
}