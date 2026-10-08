package io.miragon.blueprint.adapter.process;

import dev.bpmcrafters.processengineapi.CommonRestrictions;
import dev.bpmcrafters.processengineapi.adapter.cibseven.embedded.shared.EngineCommandExecutor;
import dev.bpmcrafters.processengineapi.task.TaskSubscriptionApi;
import dev.bpmcrafters.processengineapi.task.support.UserTaskSupport;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the process-engine-api into the embedded CIB seven engine.
 */
@Configuration
public class EngineApiConfiguration {

    /**
     * Runs engine commands (start / correlate) on the caller's thread instead of the default
     * {@code ForkJoinPool.commonPool()}, so engine and business-data share one transaction and commit or
     * roll back together. Without this, engine and business data may diverge.
     */
    @Bean
    public EngineCommandExecutor engineCommandExecutor() {
        return new EngineCommandExecutor(Runnable::run);
    }

    /**
     * In-memory pool that collects the {@code clarifyAlternative} user tasks delivered by the adapter, so
     * the outbound adapter can resolve a task's id and complete it via the UserTaskCompletionApi.
     */
    @Bean
    public UserTaskSupport clarifyAlternativeUserTaskSupport(TaskSubscriptionApi taskSubscriptionApi) {
        UserTaskSupport userTaskSupport = new UserTaskSupport();
        userTaskSupport.subscribe(
                taskSubscriptionApi,
                CommonRestrictions.builder().build(),
                FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID,
                null);
        return userTaskSupport;
    }
}
