# common-starter

## 项目概述

Spring Boot Web 应用统一启动器，聚合所有常用功能的自动配置，提供开箱即用的微服务基础能力。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-starter</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
app:
  cors:
    enabled: true
    allowed-origins:
      - https://example.com
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
    allowed-headers: "*"
    allow-credentials: true
    max-age: 3600
```

### 3. 使用方式

#### 3.1 接口返回统一格式

```java
@RestController
@RequestMapping("/api")
public class UserController {
    
    // 普通接口
    @PostMapping("/users")
    public ServerResponse<User> createUser(@RequestBody UserDto dto) {
        User user = userService.create(dto);
        return ServerResponse.success(user);
    }
    
    // 分页接口
    @PostMapping("/users/query")
    public ServerResponse<User> queryUsers(@RequestBody QueryRequest<UserQuery> query) {
        Page<User> page = userService.query(query);
        return ServerResponse.success(page);  // 自动带 pager 信息
    }
    
    // 失败响应
    @GetMapping("/users/{id}")
    public ServerResponse<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ServerResponse.fail(404, "User not found");
        }
        return ServerResponse.success(user);
    }
}
```

**响应格式：**
```json
{
    "code": 200,
    "data": {...},
    "message": "success",
    "pager": {
        "hasNext": false,
        "page": 1,
        "size": 15,
        "total": 100
    },
    "timestamp": 1736324137000,
    "traceId": "a1b2c3d4"
}
```

#### 3.2 分页查询

```java
// 前端请求
{
    "query": {
        "username": "john",
        "status": "ACTIVE"
    },
    "page": 0,
    "size": 20,
    "orderBy": [
        {"property": "createdAt", "direction": "DESC"},
        {"property": "username", "direction": "ASC"}
    ]
}

// 后端接收
@PostMapping("/query")
public ServerResponse<User> query(@RequestBody QueryRequest<UserQuery> query) {
    Page<User> page = userService.query(query);
    return ServerResponse.success(page);
}

// QueryRequest 自动处理分页和排序
// query.getPage() = 0
// query.getSize() = 20
// query.getOrderBy() = 排序条件
```

#### 3.3 异常处理

```java
// 抛出业务异常
throw new AppException("user.not.found").i18n();
throw new AppException("user.not.found").i18n(userId);

// 自动封装为 ServerResponse
{
    "code": 500,
    "message": "用户不存在",
    "traceId": "..."
}
```

#### 3.4 国际化

```java
// resources/i18n/messages_zh_CN.properties
user.not.found=用户不存在: {0}
user.already.exists=用户已存在

// resources/i18n/messages_en_US.properties
user.not.found=User not found: {0}
user.already.exists=User already exists

// 使用
throw new AppException("user.not.found").i18n(username);

// 自动根据 Accept-Language 返回对应语言
```

#### 3.5 字段加密

```java
@Entity
public class User {
    
    @Encrypt
    private String idCard;
    
    @Encrypt
    private String phone;
}

// 配置加密版本
app:
  security:
    encrypt-version: aes  # aes, base64, rsa
```

#### 3.6 健康检查

```
# 首页
GET http://localhost:8080/
# 响应: OK

# 健康检查
GET http://localhost:8080/healthz
# 响应: OK
```

## 面向 LLM

### 核心组件

1. **自动配置**
   - `CommonAutoConfig`：核心自动配置
   - `JacksonConfig`：JSON 序列化配置（FastJson2）
   - `FastJson2Support`：FastJson2 集成

2. **响应模型**
   - `ServerResponse`：统一响应结构
   - `QueryRequest`：分页查询请求（包含 query、page、size、orderBy）
   - `Pager`：分页信息

3. **异常处理**
   - `ExceptionHandle`：全局异常处理
   - `AppException`：业务异常

4. **Web 配置**
   - CORS 跨域配置
   - i18n 国际化
   - 健康检查接口

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-starter/src/main/java/com/dev/lib/`
- Git 提交：使用 `git log --oneline common-starter/` 查看
