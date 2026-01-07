# 快速开始

## 环境要求

- jdk21

## 依赖引入

springboot项目根 pom

```xml

<project>
    <parent>
        <groupId>com.dev.lib</groupId>
        <artifactId>common-lib</artifactId>
        <version>${common.version}</version>
    </parent>
</project>
```

## 最小配置
```yaml
# in your application.yaml
app:
  dubbo:
    scan-packages: @groupId@
  cloud:
    namespace: your-namespace-hsere
    host: your-
    port: 8848
    group: DEV
    username: nacos
    password: nacos
  security:
    encrypt-version: base64
  snow-flake:
    data-center-id: 0
  logstash:
    host: your-logstash-host:port
spring:
  application:
    name: @artifactId@
  config:
    import: optional:nacos:${spring.application.name}.yaml
  cloud:
    nacos:
      config:
        server-addr: ${app.cloud.host}:${app.cloud.port}
        group: ${app.cloud.group}
        namespace: ${app.cloud.namespace}
        username: ${app.cloud.username}
        password: ${app.cloud.password}
  sql:
    init:
      schema-locations: classpath:index.sql
  profiles:
    include: lib
    active: dev
  datasource:
    driver-class-name: @db.driver@
  jpa:
    database-platform: @db.dialect@
    hibernate:
      ddl-auto: update
server:
  servlet:
    context-path: /your-server-path
  port: 8080
```

```xml
<mysql>
    <db.driver>com.mysql.cj.jdbc.Driver</db.driver>
    <db.dialect>org.hibernate.dialect.MySQLDialect</db.dialect>
</mysql>
<pgsql>
    <db.driver>org.postgresql.Driver</db.driver>
    <db.dialect>org.hibernate.dialect.PostgreSQLDialect</db.dialect>
</pgsql>
<dm>
    <db.driver>org.postgresql.Driver</db.driver>
    <db.dialect>org.hibernate.dialect.PostgreSQLDialect</db.dialect>
</dm>
```

# 业务模块依赖指引

## 1. server / web 模块

## 依赖引入

```xml

<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-starter</artifactId>
</dependency>
```

## 业务说明

### 请求

```java
普通请求
@PostMapping("/api/test")
public ServerResponse<Object> function() {

    return ServerResponse.success("return result here");
}

分页请求
@PostMapping("/api/query")
public ServerResponse<Object> function(
        @RequestBody QueryRequest<your-query-class> query
) {

    return ServerResponse.success("return result here");
}
```
> 当使用 QueryRequest 时前端请求结构为
```json
{
    "query": {
        your-query-class fields
    },
    "page": page,
    "size": size,
    "orderBy": [
        {
            "property": "order-by-field",
            "direction": "ASC"
        },
        {
            "property": "order-by-field",
            "direction": "DESC"
        }
    ]
}
```
> 其中，size 最大只返回 128条，orderBy 的 property 需要是实体类的字段名

### 响应

ServerResponse的结构为

```text
{
    "code": 200,
    "data": ?, #无论是list/data/string/page，所有数据都在这里
    "message": "success",
    "pager": { #如果是分页数据，这里会展示分页信息
        "hasNext": false,
        "page": 1,
        "size": 15,
        "total": 0
    },
    "timestamp": 1766557337134, #请求完成时间
    "traceId": "bGBMngNHnZu" #日志追踪id
}
```

> 如果想要返回 pager 分页信息，需要给ServerResponse.success(Page<T>) 传入springdata 的 page 对象

### common-starter 说明

- 修改了 web 序列化机制，默认使用 fastjson2进行返回序列化
  - 修改了时间格式化
  - 修改了BigDecimal格式
  - 设置了 Long 丢失精度保护(超大 Long 会自动转 string)
- 配置了 cors 跨域，默认 AllowedOriginPattern("*") 可通过 yaml 配置

```yaml
# application.yaml
app:
  cors:
    allowedOrigins:
      - baidu.com
    allowedMethods:
      - get
```

- 配置了 i18n 国际化，通过请求头设置 Accept-Language 来切换后端响应信息的多语言。后端需要在
  resource/i18n/messages_en_US.properties进行多语言配置。
  - 普通调用
  > MessageUtils.get("app.plat.error")
  - 异常信息
  > throw new AppException("app.plat.error").i18n()
  > 
  > 如果 app.plat.error=服务异常%s, 需要返回参数
  > throw new AppException("app.plat.error").i18n("这里是说明") 
  - 注意
  > 多语言 properties 文件必须以messages_开头才能识别
- 配置了首页接口，健康检查接口。直接访问/healthz
- 配置了多版本加密解密逻辑，默认的策略有 AES，Base64，RSA，后续项目可以通过继承CustomEncryptor接口扩展
```yaml
app:
  security:
    encrypt-version: base64 / custom / rsa
```
