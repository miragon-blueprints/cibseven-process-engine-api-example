package io.miragon.blueprint.adapter.process

import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.cibseven.bpm.engine.impl.cfg.ProcessEnginePlugin
import org.springframework.context.annotation.Configuration

/**
 * Process-engine plugin that configures removal-time-based history cleanup. CIB seven auto-detects
 * beans implementing [ProcessEnginePlugin] and registers them with the embedded engine.
 */
@Configuration
class HistoryCleanupConfiguration : ProcessEnginePlugin {

    override fun preInit(configuration: ProcessEngineConfigurationImpl) {
        configuration.historyCleanupStrategy = "removalTimeBased"
        configuration.historyCleanupBatchWindowStartTime = "22:00"
        configuration.historyCleanupBatchWindowEndTime = "06:00"
        configuration.historyCleanupBatchSize = 500
        configuration.historyCleanupDegreeOfParallelism = 1
    }

    override fun postInit(configuration: ProcessEngineConfigurationImpl) {}

    override fun postProcessEngineBuild(processEngine: ProcessEngine) {}
}
