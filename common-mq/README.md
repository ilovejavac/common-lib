# MQ 消息队列使用指南

## 目录

- [概述](#概述)
- [快速开始](#快速开始)
- [发送消息](#发送消息)
- [消费消息](#消费消息)
- [ACK 机制](#ack-机制)
- [持久化与可靠性](#持久化与可靠性)
- [各 MQ 的交换机匹配机制](#各-mq-的交换机匹配机制)

## 概述

统一的 MQ 抽象层，屏蔽不同消息队列的差异。

**模块结构：**
```
common-mq/                 # 抽象层
  ├── MQ.kt                # 静态工具类
  ├── MessageExtend.kt      # 消息封装
  └── reliability/          # 可靠性配置

common-mq-rabbit/          # RabbitMQ 实现
common-local-task-message/ # 本地消息表
```

## 快速开始

### 1. 添加依赖

```xml
<!-- RabbitMQ -->
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-mq-rabbit</artifactId>
</dependency>

<!-- 包含本地消息表 -->
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-local-task-message</artifactId>
</dependency>
```

### 2. 配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 3. 发送第一条消息

```kotlin
MQ.publish("test.queue", MessageExtend.of("Hello World"))
```

## 发送消息

### 基础发送

```kotlin
// 简单发送
MQ.publish("order.queue", MessageExtend.of(order))
```

### 消息配置

```kotlin
val msg = MessageExtend.of(orderPayload)
    .key("user.123")              // routing key
    .ttl(60000)                   // 60秒过期
    .delay(5000)                  // 延迟5秒
    .priority(5)                  // 优先级 0-9
    .retry(5, 2000)               // 失败重试5次，间隔2秒
    .deadLetter("order.dlq")      // 死信队列
    .sharding("user-123")         // 分片键，保证有序

MQ.publish("order.queue", msg)
```

### 异步发送 + 回调

```kotlin
MQ.publishAsync("order.queue", msg) {
    onSuccess { m -> log.info("发送成功: ${m.id}") }
    onFailure { m, e ->
        log.error("发送失败，已落本地消息表", e)
        // 本地消息表会在定时任务中重新发送
    }
}
```

### 自定义 Header

```kotlin
val msg = MessageExtend.of(order)
msg["trace-id"] = TraceContext.id()
msg["source"] = "order-service"
MQ.publish("order.queue", msg)
```

### 可靠性配置

```kotlin
// 启动时配置
@Configuration
class MQConfig {
    @EventListener
    fun onReady(event: ApplicationReadyEvent) {
        MQ.config(ReliabilityConfig(
            enableConfirm = true,        // 启用发送确认
            enablePersistence = true,     // 持久化到磁盘
            enableDeadLetter = true,      // 启用死信
            enableStorage = true          // 启用本地消息表
        ))
    }
}
```

## 消费消息

### RabbitMQ 消费

```kotlin
@Component
class OrderHandler {

    @RabbitListener(queues = ["order.queue"])
    fun handleOrder(
        @Payload msg: MessageExtend<Order>,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) tag: Long
    ) {
        RabbitMQHandlerHelper.handleWithRetry(msg, channel, tag) {
            try {
                processOrder(msg.body)
                MQ.ack()      // 成功，删除消息
            } catch (e: Exception) {
                MQ.nack()     // 失败，重新入队重试
            }
        }
    }
}
```

### 简化消费（无需手动 ACK）

```kotlin
@RabbitListener(queues = ["order.queue"], ackMode = MANUAL)
fun handleOrder(@Payload order: Order) {
    processOrder(order)
    // 自动 ACK
}
```

## ACK 机制

### ACK 类型

| 类型 | 含义 | 使用场景 |
|------|------|----------|
| `ACK` | 确认，消息删除 | 处理成功 |
| `NACK` | 拒绝，重新入队 | 临时故障，需重试 |
| `REJECT` | 拒绝，不重入队 | 永久故障，进死信 |

### ACK 流程图

```
┌─────────────────────────────────────────┐
│            收到消息                       │
└──────────────┬──────────────────────────┘
               │
               ▼
         ┌───────────┐
         │ 业务处理   │
         └───────┬───┘
                 │
        ┌────────┴────────┐
        │                 │
     成功              失败
        │                 │
        ▼                 ▼
    MQ.ack()         retry < max?
        │                 │
        │             ┌────┴────┐
        │            Yes         No
        │             │           │
        │             ▼           ▼
        │         MQ.nack()   MQ.reject()
        │             │           │
        │             └─────┬─────┘
        │                   ▼
        │            重新入队列
        │                   │
        └───────────────────┘
```

### 自动重试

```kotlin
// MessageExtend 内置重试配置
val msg = MessageExtend.of(order)
    .retry(3, 2000)  // 最多重试3次，每次间隔2秒

// 消费时自动处理重试
RabbitMQHandlerHelper.handleWithRetry(msg, channel, tag) {
    process(msg.body)
    MQ.ack()
}

// 内部逻辑：
// 第1次失败 → NACK → 重新入队 → 2秒后重试
// 第2次失败 → NACK → 重新入队 → 2秒后重试
// 第3次失败 → NACK → 重新入队 → 2秒后重试
// 第4次失败 → REJECT → 进入死信队列
```

## 持久化与可靠性

### 三层防护

```
┌─────────────────────────────────────────────┐
│              消息可靠性保障                   │
├─────────────────────────────────────────────┤
│ 1. 生产端                                   │
│    - Confirm 回调                           │
│    - 本地消息表                              │
├─────────────────────────────────────────────┤
│ 2. 服务端 (RabbitMQ)                        │
│    - Queue 持久化                            │
│    - Message 持久化                          │
├─────────────────────────────────────────────┤
│ 3. 消费端                                   │
│    - 手动 ACK                                │
│    - 消费重试                                │
└─────────────────────────────────────────────┘
```

### 消息生命周期

```
业务代码
   │
   ▼
┌─────────────┐
│ 尝试发送MQ  │
└──────┬──────┘
       │
   ┌───┴────┐
   │        │
成功        失败
   │        │
   │        ▼
   │   ┌─────────────┐
   │   │ 本地消息表   │ ◄─── 防止丢失
   │   └──────┬──────┘
   │          │
   │          ▼
   │   定时任务补偿
   │          │
   └──────────┼──────────┐
              │          │
              ▼          ▼
        ┌──────────┐  ┌──────────┐
        │RabbitMQ  │  │ 失败告警 │
        │持久化    │  └──────────┘
        └─────┬────┘
              │
              ▼
        ┌──────────┐
        │ 消费处理  │
        └─────┬────┘
              │
         ┌────┴────┐
         │         │
      成功       失败
         │         │
         ▼         ▼
      ACK    重试/死信
```

### 配置示例

```kotlin
// 完整配置
MQ.config(ReliabilityConfig(
    enableConfirm = true,        // Confirm 回调
    enablePersistence = true,     // 消息持久化
    maxRetries = 3,               // 最大重试次数
    retryInterval = 1000,         // 重试间隔
    enableDeadLetter = true,      // 启用死信
    storage = localTaskMessageStorage  // 本地消息表
))
```

## 各 MQ 的交换机匹配机制

### RabbitMQ

```
           Exchange
              │
       ┌──────┴──────┬──────────┐
       │             │          │
    Binding       Binding    Binding
   (order.created) (order.*)  (order.*.*)
       │             │          │
       ▼             ▼          ▼
    Queue-A       Queue-B    Queue-C
```

**Exchange 类型：**

| 类型 | 说明 | 使用场景 |
|------|------|----------|
| **Direct** | 精确匹配 key | 点对点 |
| **Topic** | 通配符 `*` `#` | 发布订阅 |
| **Fanout** | 忽略 key，广播 | 广播 |
| **Headers** | 匹配 headers | 复杂路由 |

**使用示例：**

```kotlin
// 1. Direct Exchange
val msg = MessageExtend.of(order)
    .key("order.created")  // 精确匹配
MQ.publish("order.direct.exchange", msg)

// 绑定: exchange 绑定到 order-queue，key="order.created"

// 2. Topic Exchange
val msg = MessageExtend.of(order)
    .key("order.created.user123")  // 通配符匹配
MQ.publish("order.topic.exchange", msg)

// 绑定规则:
// order.*           → Queue-A  (所有订单事件)
// order.created.*   → Queue-B  (创建事件)
// order.*.user123   → Queue-C  (用户123的订单)

// 3. Fanout Exchange
val msg = MessageExtend.of(notification)
MQ.publish("notification.fanout", msg)

// 所有绑定的队列都会收到消息
```

### Kafka（没有 Exchange）

```
           Topic: order-topic
              │
       ┌──────┴──────┬──────────┐
       │             │          │
  Partition-0   Partition-1  Partition-2
       │             │          │
    hash(key)    hash(key)   hash(key)
```

**核心概念：**

| 概念 | 说明 |
|------|------|
| **Topic** | 主题，消息分类 |
| **Partition** | 分区，提高并行度 |
| **Key** | 决定路由到哪个分区（哈希） |
| **Consumer Group** | 消费者组，组内每个分区只被一个消费者消费 |

**使用示例：**

```kotlin
// Kafka 发送
val msg = MessageExtend.of(order)
    .key("user-123")  // 决定路由到哪个分区
MQ.publish("order-topic", msg)

// 同一个 key 总是路由到同一个分区 → 保证顺序

// 分区数量 = 3
// key="user-001" → Partition-0
// key="user-002" → Partition-1
// key="user-003" → Partition-2
// key="user-001" → Partition-0  (相同key，相同分区)
```

### RocketMQ

```
           Topic: order-topic
              │
       ┌──────┴──────┬──────────┐
       │             │          │
     Tag-A        Tag-B       Tag-C
       │             │          │
    Queue-0      Queue-1     Queue-2
```

**核心概念：**

| 概念 | 说明 |
|------|------|
| **Topic** | 一级分类 |
| **Tag** | 二级过滤，类似 RabbitMQ 的 routing key |
| **Queue** | 实际存储，默认 3 个 |

**使用示例：**

```kotlin
// RocketMQ 发送
val msg = MessageExtend.of(order)
    .key("order-created")  // Tag
MQ.publish("order-topic", msg)

// 消费者订阅：
// subscribe("order-topic", "order-created")  ← 只收创建订单
// subscribe("order-topic", "order-paid")    ← 只收支付订单
// subscribe("order-topic", "*")            ← 收全部订单事件
```

### 对比总结

| 特性 | RabbitMQ | Kafka | RocketMQ |
|------|----------|-------|----------|
| **Exchange** | ✅ 4种类型 | ❌ 无 | ✅ Tag 过滤 |
| **Routing** | routing key | partition key | tag |
| **顺序保证** | 单队列 | 单分区 | 单队列 |
| **持久化** | Queue + Message | 分区副本 | 同步刷盘 |
| **死信** | ✅ DLQ | ✅ Compact | ✅ DLQ |

## 常见问题

### Q1: 消息丢失了怎么办？

**A:** 启用本地消息表：

```kotlin
MQ.config(ReliabilityConfig(enableStorage = true))

// 发送失败时自动落库，定时任务补偿
```

### Q2: 消息重复消费？

**A:** 基于 `message.id` 幂等处理：

```kotlin
fun process(message: MessageExtend<Order>) {
    val id = message.id
    if (redis.setnx("processed:$id", "1", 3600) == 0) {
        return  // 已处理过，跳过
    }
    // 处理业务
}
```

### Q3: 消息积压？

**A:** 动态扩容消费者：

```kotlin
@RabbitListener(
    queues = ["order.queue"],
    concurrency = "1-10"  // 最小1，最大10
)
```

### Q4: 如何保证顺序？

**A:**

- **RabbitMQ**: 单队列 + 单消费者
- **Kafka**: 相同 key → 相同 partition
- **RocketMQ**: 单队列

### Q5: 死信消息如何处理？

**A:** 监听死信队列，人工介入或自动补偿：

```kotlin
@RabbitListener(queues = ["order.dlq"])
fun handleDlq(@Payload msg: MessageExtend<Order>) {
    log.error("订单处理失败: ${msg.body}")
    alertService.notify(msg)
}
```

## 最佳实践

### 1. 生产环境配置

```kotlin
MQ.config(ReliabilityConfig(
    enableConfirm = true,
    enablePersistence = true,
    enableStorage = true
))

// 发送重要消息
MQ.publishAsync("critical.queue", msg) {
    onFailure { m, e -> alertService.alert("发送失败", m, e) }
}
```

### 2. 消费幂等

```kotlin
fun handleMessage(msg: MessageExtend<Order>) {
    val processed = idempotentService.checkAndMark(msg.id)
    if (!processed) {
        process(msg.body)
    }
}
```

### 3. 监控告警

```kotlin
// 监控指标
- MQ 发送成功率
- 消费延迟
- 死信队列消息数
- 本地消息表积压量
```

### 4. 降级策略

```kotlin
try {
    MQ.publish("order.queue", msg)
} catch (e: Exception) {
    // 降级到本地消息表
    localTaskMessageStorage.saveAsPending(msg, "order.queue")
}
```
