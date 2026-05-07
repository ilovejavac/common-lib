# common-task-kernel

统一任务内核，支持：

- 一次性异步任务
- 一次性可靠任务
- 周期性 cron 任务
- 统一任务查询
- 统一任务管理
- 多实例 `skip locked` 抢占执行

## 定义执行器

### 异步任务

```java
@Task("EXPORT_EXCEL")
public class ExportExcelExecutor implements AsyncTaskExecutor<ExportExcelPayload> {

    @Override
    public TaskExecuteResult execute(TaskExecuteContext<ExportExcelPayload> context) {
        return TaskExecuteResult.success();
    }
}
```

### 可靠任务

```java
@Task("ORDER_NOTIFY")
public class OrderNotifyExecutor implements ReliableTaskExecutor<OrderNotifyPayload> {

    @Override
    public TaskExecuteResult execute(TaskExecuteContext<OrderNotifyPayload> context) {
        return TaskExecuteResult.success();
    }
}
```

### 周期任务

```java
@Task(value = "DAILY_REPORT", cron = "0 0 2 * * ?")
public class DailyReportExecutor implements ScheduleTaskExecutor<DailyReportPayload> {

    @Override
    public TaskExecuteResult execute(TaskExecuteContext<DailyReportPayload> context) {
        return TaskExecuteResult.success();
    }
}
```

## 提交任务

### 立即执行

```java
taskClient.submit(new ExportExcelPayload());
taskClient.submit("EXPORT_EXCEL", new ExportExcelPayload());
```

### 延迟执行

```java
taskClient.runAt(LocalDateTime.now().plusMinutes(10))
        .submit(new ExportExcelPayload());
```

### 注册周期任务

```java
taskClient.submit(new DailyReportPayload());
```

## 默认策略

### AsyncTaskExecutor

- 最大重试：2 次
- 重试间隔：10 秒固定间隔
- 重试耗尽：`FAILED`

### ReliableTaskExecutor

- 最大重试：10 次
- 重试间隔：指数退避，从 2 秒开始
- 重试耗尽：`MANUAL_REQUIRED`

## 执行语义

- 多实例并发消费通过 `select for update skip locked` 避免同一条任务记录被同时执行
- 崩溃恢复语义为 `at-least-once`
- 周期任务每次触发生成独立 `task_run`

