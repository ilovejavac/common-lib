# common-cloud

## 项目概述

微服务基础设施，提供 Dubbo 配置、服务间调用、用户上下文传递、链路追踪等功能。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
app:
  dubbo:
    scan-packages: com.example.service  # 服务扫描包
    application-name: ${spring.application.name}
    protocol:
      name: dubbo
      port: 20880
    registry:
      address: nacos://localhost:8848
```

### 3. 定义 Dubbo 服务

```java
// 服务接口
public interface UserService {
    User getUserById(Long id);
}

// 服务实现（提供者）
@Service(version = "1.0.0")
@DubboService
public class UserServiceImpl implements UserService {
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

### 4. 调用 Dubbo 服务

```java
// 服务引用（消费者）
@Service
public class OrderService {
    
    @DubboReference(version = "1.0.0")
    private UserService userService;
    
    public Order createOrder(OrderDto dto) {
        User user = userService.getUserById(dto.getUserId());
        // 用户上下文自动传递
    }
}
```

### 5. 用户上下文传递

```java
// 服务端获取用户上下文
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public User getUserById(Long id) {
        // 用户上下文自动传递
        String userId = UserContextHolder.getUserId();
        String traceId = UserContextHolder.getTraceId();
        
        return userRepository.findById(id).orElse(null);
    }
}
```

## 面向 LLM

### 核心组件

1. **配置**
   - `AppCloudConfig`：云配置
   - `AppDubboProperties`：Dubbo 属性配置

2. **过滤器**
   - `UserContextProviderFilter`：提供者用户上下文
   - `UserContextConsumerFilter`：消费者用户上下文
   - `DubboTraceProviderFilter`：提供者链路追踪
   - `DubboTraceConsumerFilter`：消费者链路追踪

3. **虚拟线程池**
   - `VirtualThreadPool`：Dubbo 虚拟线程池

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-cloud/src/main/java/com/dev/lib/cloud/`
- Git 提交：使用 `git log --oneline common-cloud/` 查看
