package io.miragon.blueprint.adapter.process;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.cibseven.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.context.annotation.Configuration;

/**
 * Process-engine plugin that configures removal-time-based history cleanup. CIB seven auto-detects
 * beans implementing {@link ProcessEnginePlugin} and registers them with the embedded engine.
 */
@Configuration
public class HistoryCleanupConfiguration implements ProcessEnginePlugin {

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        configuration.setHistoryCleanupStrategy("removalTimeBased");
        configuration.setHistoryCleanupBatchWindowStartTime("22:00");
        configuration.setHistoryCleanupBatchWindowEndTime("06:00");
        configuration.setHistoryCleanupBatchSize(500);
        configuration.setHistoryCleanupDegreeOfParallelism(1);
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl configuration) {
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
    }
}
