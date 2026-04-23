# BaseRepository Update Builder 设计说明

## 1. 背景与目标

当前 `BaseRepository` 只提供 query/count/delete 能力，缺少统一的条件更新入口。

目标是新增一个类型安全、可控的更新链式 API：

```java
repo.update()
    .set(UserPo::getName, "xxx")
    .setNull(UserPo::getDescription)
    .where(dslQuery, extraExpr...)
    .execute();
```

重点约束：

- `set(field, null)` 必须跳过，不得把字段更新为 `NULL`
- 需要置空必须显式调用 `setNull(field)`
- 仅 `execute()` 才真正写库
- `@Encrypt` 字段在 `set` 时自动执行加密判断与处理
- 默认自动补齐审计字段：`updatedAt`、`modifierId`

## 2. API 草案

### 2.1 BaseRepository 扩展

```java
UpdateBuilder<T> update();
```

### 2.2 UpdateBuilder 链式接口

```java
UpdateBuilder<T> set(SFunction<T, ?> field, Object value);
UpdateBuilder<T> setNull(SFunction<T, ?> field);
UpdateBuilder<T> where(DslQuery<T> dslQuery, BooleanExpression... expressions);
long execute();
```

## 3. 语义约束

### 3.1 set / setNull 行为

- `set(field, null)`：忽略该字段（不产生 `SET field = NULL`）
- `setNull(field)`：显式产生 `SET field = NULL`
- 同一字段多次赋值时，后者覆盖前者

### 3.2 where 与 execute

- `where(...)` 仅构建条件，不触发执行
- 必须显式 `execute()` 执行更新
- 未提供 `where(...)` 时拒绝执行（防止全表误更新）
- 若 `where(...)` 中 `dslQuery + BooleanExpression` 解析结果为空条件，`execute()` 也必须拒绝执行

### 3.3 审计字段默认补齐

执行前若调用方未显式设置：

- 自动补 `updatedAt = LocalDateTime.now()`
- 自动补 `modifierId = SecurityContextHolder.getUserId()`

说明：这是框架补偿行为，不等价于触发实体监听器。

### 3.4 @Encrypt 自动处理

对 `set(field, value)`：

- 若字段标注 `@Encrypt` 且 `value != null`：
  - `value` 必须是 `String`
  - 调用加密服务后再写入数据库
- 若 `value == null`：按 `set(null)` 规则直接跳过

### 3.5 字段解析缓存

- `set(SFunction...)` 解析字段名后，不允许每次都循环实体字段做反射查找
- 必须基于“实体类 + 字段名”做元数据缓存（例如：字段类型、是否 `@Encrypt`）
- 缓存要求线程安全（建议 `ConcurrentHashMap`）
- 解析字段时需支持父类字段（如 `JpaEntity` 基类字段），并将结果一并缓存
- 缓存未命中时才做一次反射扫描，命中后直接复用

## 4. 类型兼容性说明

### 4.1 Enum 字段

对于 `@Enumerated(EnumType.STRING)` 字段，要求调用方传入枚举实例：

```java
set(Po::getExecuteState, ExecuteState.RUNNING)
```

底层应按 Hibernate/JPA 映射规则写入字符串枚举值。

### 4.2 JSON 字段

对于 `@JdbcTypeCode(SqlTypes.JSON)` 字段，要求调用方传入该字段声明类型可接受的对象（如 `Map<String, Object>` 或约定 POJO），底层走 Hibernate JSON 映射序列化。

## 5. 执行与事务行为

- 底层使用 Querydsl `update` 执行 bulk update
- `execute()` 返回影响行数，支持 CAS 语义（`0/1`）
- 影响行数大于 0 时，执行 `flush + clear`，避免当前持久化上下文读到旧值

## 6. 非目标（明确不做）

- 不提供 `where(...).ex(...)` 的隐式执行变体
- 不在本阶段支持 `skip locked`（bulk update 不直接支持）
- 不把 bulk update 伪装为会触发 `@PreUpdate` 生命周期回调

## 7. 风险与取舍

### 7.1 已知风险

- bulk update 不触发实体监听器，无法自动执行监听器里除审计字段外的自定义逻辑
- 条件表达不当可能导致批量误更新（通过强制 where 降低风险）

### 7.2 取舍结论

- 选择“显式 execute + 强制 where + 默认审计补齐 + Encrypt 自动处理”
- 牺牲部分底层灵活性，换取统一行为与安全护栏

## 8. 验收标准

- 能使用 `update().set(...).setNull(...).where(...).execute()` 完成条件更新
- `set(null)` 不会写 `NULL`
- `setNull` 能写入 `NULL`
- `@Encrypt` 字段在 bulk update 中可自动加密
- enum/json 字段通过实体映射正确入库/回读
- `updatedAt/modifierId` 自动补齐生效
- `execute()` 返回影响行数正确
- 同一实体类重复 `update().set(...)` 不会重复反射扫描字段元数据
