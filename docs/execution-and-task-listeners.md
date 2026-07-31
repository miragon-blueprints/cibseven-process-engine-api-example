# Execution & Task Listeners with the process-engine-api

This blueprint drives CIB seven through the [process-engine-api](https://github.com/bpm-crafters)
abstraction. That layer covers **service-task work** (`@ProcessEngineWorker` beans, external-task
pattern) and **user-task delivery** (`TaskSubscriptionApi` / `UserTaskSupport`, an asynchronous
pull-based view of open tasks). 

It deliberately has **no listener concept** — neither execution
listeners (activity/process `start`/`end`, `DelegateExecution`) nor task listeners (a synchronous,
in-transaction hook on `create` / `assignment` / `complete` / `delete`, `DelegateTask`).

Because the engine is embedded, the native CIB seven listener interfaces
(`org.cibseven.bpm.engine.delegate.ExecutionListener` / `TaskListener`, wired via
`camunda:executionListener` / `camunda:taskListener`) remain available alongside the API. So reacting
to lifecycle events is a trade-off:

- **Stay within the API** — one engine-agnostic model; the engine stays replaceable. But genuine
  listener semantics are not available: no execution-level events, and only an asynchronous,
  out-of-transaction view of user tasks rather than an in-transaction hook.

- **Add native listeners** — full BPMN listener capability, synchronous and in-transaction with
  access to `DelegateExecution` / `DelegateTask`. But it binds that code to CIB seven and introduces
  a second engine-facing style next to the abstraction, which can accumulate into coupling over time.

The tension is **abstraction and exchangeability** on one side versus **binding and more capability**
on the other. This blueprint keeps everything on the API; a project that needs true listener
semantics can add native listeners with that trade-off in mind.
