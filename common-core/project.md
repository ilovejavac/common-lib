# common-core

## 项目概述

核心工具库和领域模型，提供通用工具类、设计模式实现、实体基类、DSL 查询参数等功能。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-core</artifactId>
</dependency>
```

### 2. DSL 查询参数

```java
// 定义查询参数（继承 DslQuery）
public class UserQuery extends DslQuery<User> {
    
    // 基础字段（DslQuery 已包含）
    // private Long id;
    // private String bizId;
    // private Long creator;
    // private Long modifier;
    
    // 自定义查询字段
    @Condition(field = "username")
    private String username;
    
    @Condition(field = "status")
    private UserStatus status;
    
    @Condition(type = QueryType.GTE, field = "age")
    private Integer minAge;
    
    @Condition(type = QueryType.LIKE, field = "email")
    private String email;
    
    // 不参与查询的字段
    @ConditionIgnore
    private String sortStr;  // 排序：createdAt_desc,username_asc
    
    @ConditionIgnore
    private Integer offset;  // 分页起始
    
    @ConditionIgnore
    private Integer limit;   // 分页大小
}

// Controller 中使用
@PostMapping("/users/query")
public ServerResponse<List<User>> query(@RequestBody QueryRequest<UserQuery> query) {
    // query.getQuery() 自动映射为 UserQuery
    Page<User> page = userService.query(query);
    return ServerResponse.success(page);
}
```

### 3. 工具类使用

#### 3.1 重试器 Retryer

```java
// 基础用法
String result = Retryer.builder()
    .maxAttempts(3)
    .delay(Duration.ofMillis(500))
    .retryOn(IOException.class, TimeoutException.class)
    .execute(() -> riskyApiCall());

// 指数退避
String result = Retryer.builder()
    .maxAttempts(5)
    .delay(Duration.ofMillis(100))
    .backoff(Retryer.BackoffStrategy.EXPONENTIAL)
    .backoffMultiplier(2.0)
    .maxDelay(Duration.ofSeconds(10))
    .execute(() -> apiCall());

// 自定义重试条件
Retryer.builder()
    .retryIf(e -> e.getMessage().contains("temporarily unavailable"))
    .onRetry((attempt, e) -> log.warn("Retry {}, error: {}", attempt, e.getMessage()))
    .execute(() -> operation());

// 无返回值
Retryer.builder()
    .maxAttempts(3)
    .executeVoid(() -> cleanup());
```

#### 3.2 熔断器 CircuitBreaker

```java
// 获取熔断器
CircuitBreaker breaker = CircuitBreakerRegistry.get("external-api");

// 执行操作
try {
    breaker.execute(() -> externalApi.call());
} catch (CircuitBreakerOpenException e) {
    log.error("Circuit breaker is open");
}

// 自定义配置
CircuitBreaker customBreaker = CircuitBreaker.builder()
    .failureThreshold(5)
    .successThreshold(3)
    .timeout(Duration.ofSeconds(60))
    .build();
```

#### 3.3 状态机 StateMachine

```java
// 定义状态和事件
enum OrderState { PENDING, PAID, SHIPPED, COMPLETED, CANCELLED }
enum OrderEvent { PAY, SHIP, COMPLETE, CANCEL }

// 创建状态机
StateMachine<OrderState, OrderEvent> sm = StateMachine.builder(OrderState.PENDING)
    .transition(PENDING, PAY, PAID)
    .transition(PAID, SHIP, SHIPPED)
    .transition(SHIPPED, COMPLETE, COMPLETED)
    .transition(PENDING, CANCEL, CANCELLED)
    .onTransition((from, to, event) -> 
        log.info("Order state changed: {} -> {}", from, to))
    .build();

// 触发事件
sm.fire(OrderEvent.PAY);   // PENDING -> PAID
sm.fire(OrderEvent.SHIP);  // PAID -> SHIPPED
```

#### 3.4 管道 Pipeline

```java
// 定义处理管道
Pipeline<String, String> pipeline = Pipeline.<String, String>builder()
    .stage("validate", input -> {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Invalid input");
        }
        return input;
    })
    .stage("transform", String::toUpperCase)
    .stage("prefix", input -> "PROCESSED: " + input)
    .build();

// 执行管道
String result = pipeline.execute("hello");  // "PROCESSED: HELLO"
```

#### 3.5 规约模式 Specification

```java
// 定义规约
Specification<User> adult = user -> user.getAge() >= 18;
Specification<User> active = user -> user.getStatus() == Status.ACTIVE;

// 组合规约
Specification<User> adultAndActive = adult.and(active);
Specification<User> adultOrActive = adult.or(active);
Specification<User> notAdult = adult.negate();

// 使用
List<User> filtered = users.stream()
    .filter(adultAndActive::isSatisfiedBy)
    .toList();
```

#### 3.6 策略模式 Strategies

```java
// 定义策略
Strategies<PaymentType, PaymentResult> strategies = Strategies.<PaymentType, PaymentResult>builder()
    .register(PaymentType.WECHAT, new WechatPaymentProcessor())
    .register(PaymentType.ALIPAY, new AlipayPaymentProcessor())
    .register(PaymentType.CREDIT_CARD, new CreditCardProcessor())
    .build();

// 执行策略
PaymentResult result = strategies.execute(PaymentType.WECHAT, paymentRequest);
```

#### 3.7 规则引擎 RuleEngine

```java
// 定义规则
RuleEngine<Order, Discount> engine = RuleEngine.<Order, Discount>builder()
    .rule(order -> order.getAmount() > 1000, 
          order -> new Discount(0.1, "满1000减10%"))
    .rule(order -> order.getMemberLevel() == VIP,
          order -> new Discount(0.15, "VIP会员15%折扣"))
    .rule(order -> order.isFirstOrder(),
          order -> new Discount(0.05, "首单5%优惠"))
    .build();

// 执行规则（按顺序匹配第一个）
Discount discount = engine.evaluate(order);
```

#### 3.8 HTTP 客户端 GenericHttpGateway

```java
@Autowired
private GenericHttpGateway httpGateway;

// GET 请求
String response = httpGateway.get("https://api.example.com/users")
    .param("page", "1")
    .param("size", "10")
    .header("Authorization", "Bearer token")
    .execute();

// POST 请求
String response = httpGateway.post("https://api.example.com/users")
    .header("Content-Type", "application/json")
    .body("{\"name\":\"John\"}")
    .execute();
```

### 4. 实体基类

```java
// CoreEntity 包含通用字段
public class User extends CoreEntity {
    private String username;
    
    // 自动包含：
    // private Long id;
    // private EntityStatus status;
    // private LocalDateTime createdAt;
    // private LocalDateTime updatedAt;
    // private Long creatorId;
    // private Long modifierId;
}
```

## 面向 LLM

### 核心组件

1. **DslQuery**：查询参数基类
   - `@Condition`：标记查询字段
   - `@ConditionIgnore`：标记非查询字段
   - `toSort()`：转换为 Sort
   - `toPageable()`：转换为 Pageable

2. **设计模式**
   - `StateMachine`：状态机
   - `Pipeline`：管道模式
   - `Specification`：规约模式
   - `Strategies`：策略模式
   - `RuleEngine`：规则引擎

3. **工具类**
   - `Retryer`：重试器
   - `CircuitBreaker`：熔断器
   - `GenericHttpGateway`：HTTP 客户端

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-core/src/main/java/com/dev/lib/`
- Git 提交：使用 `git log --oneline common-core/` 查看
