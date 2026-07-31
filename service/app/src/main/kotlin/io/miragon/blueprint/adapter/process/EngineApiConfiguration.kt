package io.miragon.blueprint.adapter.process

import dev.bpmcrafters.processengineapi.adapter.cibseven.embedded.shared.EngineCommandExecutor
import dev.bpmcrafters.processengineapi.task.TaskSubscriptionApi
import dev.bpmcrafters.processengineapi.task.support.UserTaskSupport
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires the process-engine-api into the embedded CIB seven engine.
 */
@Configuration
class EngineApiConfiguration {

    /**
     * Runs engine commands (start / correlate) on the caller's thread instead of the default
     * `ForkJoinPool.commonPool()`, so engine and business-data share one transaction and commit or
     * roll back together. Without this, engine and business data may diverge.
     */
    @Bean
    fun engineCommandExecutor() = EngineCommandExecutor(
        executor = { it.run() },
    )

    /**
     * In-memory pool that collects the `clarifyAlternative` user tasks delivered by the adapter, so
     * the outbound adapter can resolve a task's id and complete it via the UserTaskCompletionApi.
     */
    @Bean
    fun clarifyAlternativeUserTaskSupport(taskSubscriptionApi: TaskSubscriptionApi): UserTaskSupport =
        UserTaskSupport().apply {
            subscribe(
                taskSubscriptionApi = taskSubscriptionApi,
                taskDescriptionKey = Elements.USER_TASK_CLARIFY_ALTERNATIVE.value,
            )
        }
}
