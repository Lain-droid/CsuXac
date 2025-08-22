package com.csuxac

import com.csuxac.config.CsuXacConfig
import com.csuxac.core.SecurityEngine
import com.csuxac.core.cluster.ClusterManager
import com.csuxac.core.monitoring.MonitoringDashboard
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertTrue

class CsuXacTest {

    @Test
    fun `test configuration loading`() {
        val config = CsuXacConfig.load()
        assertTrue(config.security.strictMode)
        assertTrue(config.monitoring.enabled)
        assertTrue(config.ai.enabled)
    }

    @Test
    fun `test security engine startup and shutdown`() {
        val config = CsuXacConfig.load()
        val securityEngine = SecurityEngine(config.security)
        
        assertDoesNotThrow {
            securityEngine.start()
            assertTrue(securityEngine.isRunning())
            
            securityEngine.stop()
            assertTrue(!securityEngine.isRunning())
        }
    }

    @Test
    fun `test cluster manager startup and shutdown`() {
        val config = CsuXacConfig.load()
        val clusterManager = ClusterManager(config.cluster)
        
        assertDoesNotThrow {
            clusterManager.start()
            assertTrue(clusterManager.isRunning())
            
            clusterManager.stop()
            assertTrue(!clusterManager.isRunning())
        }
    }

    @Test
    fun `test monitoring dashboard startup and shutdown`() {
        val config = CsuXacConfig.load()
        val monitoringDashboard = MonitoringDashboard(config.monitoring)
        
        assertDoesNotThrow {
            monitoringDashboard.start()
            assertTrue(monitoringDashboard.isRunning())
            
            monitoringDashboard.stop()
            assertTrue(!monitoringDashboard.isRunning())
        }
    }

    @Test
    fun `test full system startup and shutdown`() = runBlocking {
        val app = CsuXacApplication()
        
        assertDoesNotThrow {
            app.start()
            app.shutdown()
        }
    }
}