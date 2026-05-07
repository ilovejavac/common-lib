# Common Task Kernel Redesign Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace `common-local-task-message` with a new `common-task-kernel` module that supports one-shot tasks and cron-based schedule tasks, unified task query/management, external scheduler adapters, and multi-instance safe execution.

**Architecture:** The redesign splits task definition, payload snapshot, and execution runs into separate models. Business code provides `AsyncTaskExecutor<T>`, `ReliableTaskExecutor<T>`, or `ScheduleTaskExecutor<T>` implementations annotated with `@Task`; the kernel stores task metadata in DB, resolves executor by `payload.class`, creates runs, and executes them through a `skip locked` worker loop. Internal cron and future `xxl/quartz/elasticjob` integrations only trigger schedule runs and do not execute business logic directly.

**Tech Stack:** Spring Boot auto-configuration, Java, JPA/common-data-jpa, JSON payload persistence, DB row locking with `select for update skip locked`.

---

### Task 1: Replace module entry and dependency wiring

**Files:**
- Modify: `pom.xml`
- Create: `common-task-kernel/pom.xml`
- Modify: `common-mq/pom.xml`
- Modify: `common-mq-rabbit/pom.xml`
- Modify: `common-mq-kafka/pom.xml`
- Modify: `common-mq-rocketmq/pom.xml`
- Delete: `common-local-task-message/pom.xml`

**Step 1: Replace the root module reference**

- In `pom.xml`, replace `<module>common-local-task-message</module>` with `<module>common-task-kernel</module>`.

**Step 2: Create the new module skeleton**

- Create `common-task-kernel/pom.xml`.
- Keep dependencies minimal:
  - `common-core`
  - optional `common-data-jpa`
  - optional `common-mq` only if actually needed during migration
  - Spring Boot configuration processor / Lombok inherited from parent

**Step 3: Update module consumers**

- Replace `common-local-task-message` artifact references with `common-task-kernel` in MQ-related modules and any direct consumers discovered during implementation.

**Step 4: Remove old module packaging**

- Delete old `common-local-task-message/pom.xml` after the new module is in place.

**Step 5: Commit**

```bash
git add pom.xml common-task-kernel/pom.xml common-mq/pom.xml common-mq-rabbit/pom.xml common-mq-kafka/pom.xml common-mq-rocketmq/pom.xml
git commit -m "refactor: replace local task message module with task kernel"
```

### Task 2: Define the public kernel API and annotations

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/annotation/Task.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/TaskClient.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/TaskQueryService.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/TaskManageService.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/TaskExecutor.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/AsyncTaskExecutor.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/ReliableTaskExecutor.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/ScheduleTaskExecutor.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/TaskExecuteContext.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/TaskExecuteResult.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/TaskMode.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/TaskStatus.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/TaskRunStatus.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/domain/RetryPolicy.java`

**Step 1: Define the annotation**

- `@Task(value, cron)`:
  - `value`: optional task type
  - `cron`: optional cron expression; non-empty means schedule mode

**Step 2: Define the core enums**

- `TaskMode`: `ONCE`, `SCHEDULE`
- `TaskStatus`: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `MANUAL_REQUIRED`, `CANCELED`, `ENABLED`, `PAUSED`, `DISABLED`
- `TaskRunStatus`: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `INTERRUPTED`, `CANCELED`

**Step 3: Define the execution API**

- `TaskExecutor<T>` with `execute(TaskExecuteContext<T>)`.
- `TaskExecuteResult` factory methods:
  - `success()`
  - `success(Object resultRef)`
  - `failure(String message)`
  - `failureNoRetry(String message)`

**Step 4: Define submit/query/manage interfaces**

- `TaskClient.submit(payload)`
- `TaskClient.submit(taskType, payload)`
- `TaskClient.runAt(...).submit(payload)`
- `TaskClient.runAt(...).submit(taskType, payload)`
- Query and manage APIs per agreed design

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task
git commit -m "feat: define task kernel public api"
```

### Task 3: Define persistence models and repositories

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPo.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPayloadPo.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskRunPo.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskRepository.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPayloadRepository.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskRunRepository.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPersistenceService.java`

**Step 1: Model the `task` table**

- Include:
  - `taskId`
  - `application`
  - `taskType`
  - `mode`
  - `status`
  - `payloadId`
  - `cron`
  - `runAt`
  - `retryPolicy`
  - `onExhaustedStatus`
  - `lastRunId`
  - `lastRunStatus`
  - `lastRunAt`
  - `nextRunAt`
  - `errorMessage`
  - `configHash`

**Step 2: Model the `task_payload` table**

- Persist:
  - `payloadId`
  - `taskId`
  - `payloadClass`
  - `payloadJson`
  - `payloadHash`
  - `version`

**Step 3: Model the `task_run` table**

- Persist:
  - `runId`
  - `taskId`
  - `payloadId`
  - `attemptNo`
  - `status`
  - `triggerSource`
  - `triggerTime`
  - `scheduledAt`
  - `startedAt`
  - `finishedAt`
  - `nextRetryAt`
  - `workerId`
  - `leaseExpireAt`
  - `heartbeatAt`
  - `errorMessage`
  - `errorStack`

**Step 4: Add repository methods for locking and state transitions**

- Claim runnable rows with `select for update skip locked`
- Find expired `RUNNING` runs
- Find enabled schedules due for trigger
- Insert schedule runs with unique `(taskId, triggerTime)` protection

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/infra/data
git commit -m "feat: add task kernel persistence models"
```

### Task 4: Implement executor registry and payload routing

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/registry/TaskExecutorDescriptor.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/registry/TaskExecutorRegistry.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/registry/TaskExecutorScanner.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java`

**Step 1: Scan all `TaskExecutor<?>` beans**

- Use Spring bean list + `ResolvableType` to resolve generic payload class.

**Step 2: Build descriptors**

- For each executor, compute:
  - `application`
  - `payloadClass`
  - `taskType`
  - `mode`
  - `cron`
  - bean reference

**Step 3: Build two indexes**

- `payloadClass -> descriptors`
- `(payloadClass, taskType) -> descriptor`

**Step 4: Enforce startup validation**

- Reject duplicate `(application, payloadClass, taskType)` registrations.
- Infer async/reliable defaults from executor interface type.
- Reject blank `cron` on schedule executors.
- Reject ambiguous schedule metadata if future adapters add extra constraints.

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/biz/registry common-task-kernel/src/main/java/com/dev/lib/task/config
git commit -m "feat: add task executor registry"
```

### Task 5: Implement submit flow for one-shot and schedule tasks

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/launcher/DefaultTaskClient.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/launcher/TaskSubmissionService.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java`

**Step 1: Resolve executor from payload**

- `submit(payload)`:
  - 0 descriptors -> throw not found
  - 1 descriptor -> use it
  - >1 descriptors -> throw ambiguity; require `taskType`

**Step 2: Derive mode**

- `cron` present -> `SCHEDULE`
- `cron` absent -> `ONCE`

**Step 3: Apply default policies**

- `ASYNC` preset:
  - max retry `2`
  - fixed delay `10s`
  - exhausted -> `FAILED`
- `RELIABLE` preset:
  - max retry `10`
  - exponential backoff from `2s`
  - exhausted -> `MANUAL_REQUIRED`

**Step 4: Persist task + payload + initial run**

- `ONCE`:
  - write `task`
  - write `task_payload`
  - write initial `task_run` with `PENDING`
- `SCHEDULE`:
  - write `task`
  - write `task_payload`
  - compute `nextRunAt`
  - do not create immediate run unless explicitly triggered later

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/biz/launcher common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java
git commit -m "feat: add task submission flow"
```

### Task 6: Implement worker dispatcher and retry engine

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/dispatcher/TaskDispatcher.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/dispatcher/TaskWorkerLoop.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/dispatcher/TaskRetryCalculator.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/dispatcher/TaskExecutionService.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPersistenceService.java`

**Step 1: Claim runnable runs**

- Query `PENDING` runs due now using `select for update skip locked`.
- Mark claimed runs `RUNNING`.
- Write `workerId`, `startedAt`, `leaseExpireAt`.

**Step 2: Execute task**

- Load `task`, `payload`, `executor`.
- Deserialize payload JSON into the resolved payload class.
- Call `executor.execute(context)`.

**Step 3: Handle success**

- Mark current `task_run` `SUCCESS`.
- Update `task.lastRunStatus`, `task.lastRunAt`.
- For `ONCE`, move `task.status` to `SUCCESS`.
- For `SCHEDULE`, keep `task.status = ENABLED`.

**Step 4: Handle failure**

- Record error fields.
- If retry budget remains:
  - mark current run `FAILED`
  - create next retry run with computed `nextRetryAt`
- If budget exhausted:
  - `ASYNC` -> `task.status = FAILED`
  - `RELIABLE` -> `task.status = MANUAL_REQUIRED`
  - `SCHEDULE` -> keep task enabled, only fail this run

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/biz/dispatcher common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPersistenceService.java
git commit -m "feat: add task dispatch and retry engine"
```

### Task 7: Implement schedule triggering and config refresh

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/service/ScheduleTriggerService.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/service/ScheduleRefreshService.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/ScheduleTriggerGateway.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/infra/trigger/internal/InternalScheduleTriggerJob.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java`

**Step 1: Expose trigger gateway**

- `fire(taskId, triggerTime, triggerSource)` inserts a pending run with unique `(taskId, triggerTime)`.

**Step 2: Implement internal cron support**

- Periodically scan enabled tasks with `nextRunAt <= now`.
- Call trigger gateway.
- Recompute and persist the next trigger time.

**Step 3: Implement refresh-on-startup**

- Scan registered schedule executors.
- Compare current executor config hash with stored task config hash.
- If changed, update:
  - `cron`
  - `retryPolicy`
  - `onExhaustedStatus`
  - `nextRunAt`

**Step 4: Keep adapter seam small**

- Internal trigger lives in kernel now.
- Future `xxl/quartz/elasticjob` modules should only depend on `ScheduleTriggerGateway` and reuse the same trigger path.

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/biz/service common-task-kernel/src/main/java/com/dev/lib/task/api/ScheduleTriggerGateway.java common-task-kernel/src/main/java/com/dev/lib/task/infra/trigger/internal common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java
git commit -m "feat: add schedule triggering and refresh"
```

### Task 8: Implement restart recovery and task management

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/recovery/TaskRecoveryService.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/service/DefaultTaskManageService.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPersistenceService.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java`

**Step 1: Recover expired running runs**

- On startup, find `RUNNING` runs with expired lease.
- Mark them `INTERRUPTED`.

**Step 2: Resume one-shot tasks**

- If retry budget remains, create a new retry run.
- Otherwise:
  - `ASYNC` -> `FAILED`
  - `RELIABLE` -> `MANUAL_REQUIRED`

**Step 3: Resume schedule tasks**

- Leave parent task `ENABLED`.
- Let the next trigger create the next run.

**Step 4: Implement manage operations**

- `cancel(taskId)`
- `retry(taskId)`
- `pause(taskId)`
- `resume(taskId)`
- `triggerNow(taskId)`
- `refreshSchedule(taskId)`

**Step 5: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/biz/recovery common-task-kernel/src/main/java/com/dev/lib/task/biz/service common-task-kernel/src/main/java/com/dev/lib/task/infra/data/TaskPersistenceService.java common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java
git commit -m "feat: add recovery and task management"
```

### Task 9: Implement unified query views

**Files:**
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/biz/service/DefaultTaskQueryService.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/view/TaskView.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/view/TaskRunView.java`
- Create: `common-task-kernel/src/main/java/com/dev/lib/task/api/query/TaskQuery.java`
- Modify: `common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java`

**Step 1: Define query views**

- `TaskView` should expose:
  - task identity
  - mode
  - status
  - created/updated time
  - last run status/time
  - next run time
  - latest error summary

**Step 2: Implement task and run queries**

- `getTask`
- `pageTasks`
- `listRuns`
- `getLastRun`

**Step 3: Keep the query layer mode-aware**

- For `ONCE`, show final status and last retry info.
- For `SCHEDULE`, show enablement status plus run history.

**Step 4: Commit**

```bash
git add common-task-kernel/src/main/java/com/dev/lib/task/api/view common-task-kernel/src/main/java/com/dev/lib/task/api/query common-task-kernel/src/main/java/com/dev/lib/task/biz/service common-task-kernel/src/main/java/com/dev/lib/task/config/TaskKernelAutoConfig.java
git commit -m "feat: add task query service"
```

### Task 10: Remove old module code and add migration notes

**Files:**
- Delete: `common-local-task-message/src/main/java/com/dev/lib/local/task/message/**`
- Delete: `common-local-task-message/src/test/**` if present
- Modify: `common-mq/README.md`
- Create: `common-task-kernel/README.md`

**Step 1: Remove old implementation**

- Delete legacy poller engine, annotations, storage adapter, config classes, and entities from `common-local-task-message`.

**Step 2: Add migration notes**

- In `common-task-kernel/README.md`, document:
  - `AsyncTaskExecutor<T>` / `ReliableTaskExecutor<T>` / `ScheduleTaskExecutor<T>`
  - `@Task`
  - `submit(payload)`
  - `submit(taskType, payload)`
  - `runAt(time).submit(payload)`
  - async/reliable defaults
  - schedule behavior
  - at-least-once semantics

**Step 3: Update MQ docs**

- Replace mentions of `common-local-task-message` with `common-task-kernel`.
- Clarify that MQ reliable delivery now uses the new one-shot task kernel.

**Step 4: Commit**

```bash
git add common-task-kernel/README.md common-mq/README.md
git rm -r common-local-task-message/src/main/java/com/dev/lib/local/task/message
git commit -m "refactor: remove legacy local task message implementation"
```

### Task 11: Add focused tests for kernel behavior

**Files:**
- Create: `common-task-kernel/src/test/java/com/dev/lib/task/registry/TaskExecutorRegistryTest.java`
- Create: `common-task-kernel/src/test/java/com/dev/lib/task/launcher/TaskSubmissionServiceTest.java`
- Create: `common-task-kernel/src/test/java/com/dev/lib/task/dispatcher/TaskRetryCalculatorTest.java`
- Create: `common-task-kernel/src/test/java/com/dev/lib/task/dispatcher/TaskExecutionServiceTest.java`
- Create: `common-task-kernel/src/test/java/com/dev/lib/task/service/ScheduleTriggerServiceTest.java`
- Create: `common-task-kernel/src/test/java/com/dev/lib/task/recovery/TaskRecoveryServiceTest.java`

**Step 1: Registry tests**

- one payload -> one executor -> submit resolves successfully
- one payload -> many executors -> submit requires taskType
- duplicate typed registration -> startup validation fails

**Step 2: Submission tests**

- async defaults applied
- reliable defaults applied
- cron task stored as schedule task
- `runAt` only affects one-shot tasks

**Step 3: Retry tests**

- fixed `10s` retry schedule for async
- exponential retry schedule for reliable
- exhausted transitions to `FAILED` vs `MANUAL_REQUIRED`

**Step 4: Recovery tests**

- expired running once-task becomes retry or final status
- expired running schedule run becomes interrupted and parent remains enabled

**Step 5: Commit**

```bash
git add common-task-kernel/src/test/java/com/dev/lib/task
git commit -m "test: cover task kernel core behaviors"
```
