# common-data-jpa

## 项目概述

JPA 数据访问层增强，提供软删除、审计字段、QueryDSL 集成、链式查询等功能。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-data-jpa</artifactId>
</dependency>
```

### 2. 定义实体

```java
@Entity
public class User extends JpaEntity {
    
    private String username;
    
    private String email;
    
    private UserStatus status;
    
    // JpaEntity 自动包含：
    // private Long id;
    // private LocalDateTime createdAt;
    // private LocalDateTime updatedAt;
    // private Long creatorId;
    // private Long modifierId;
}
```

### 3. 定义 Repository

```java
public interface UserRepository extends BaseRepository<User> {
    
    // 无需定义基础方法，BaseRepository 已包含：
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)  // 软删除
}
```

### 4. 使用 QueryBuilder

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 基础查询
    public User findById(Long id) {
        return userRepository.load(QUser.user.id.eq(id)).orElse(null);
    }
    
    // 条件查询
    public List<User> findByStatus(UserStatus status) {
        return userRepository.loads(QUser.user.status.eq(status));
    }
    
    // 多条件查询
    public List<User> findActiveUsers(String keyword) {
        return userRepository.loads(
            QUser.user.status.eq(UserStatus.ACTIVE)
            .and(QUser.user.username.contains(keyword))
        );
    }
    
    // 分页查询
    public Page<User> query(UserQuery query, int page, int size) {
        return userRepository.page(query)
            .where(
                QUser.user.status.eq(query.getStatus()),
                QUser.user.username.containsIgnoreCase(query.getKeyword())
            )
            .page(page, size);
    }
    
    // 只查询指定字段（返回 DTO）
    public List<UserDto> findUserDtos() {
        return userRepository.select(QUser.user.id, QUser.user.username)
            .loads(UserDto.class);
    }
    
    // 悲观锁查询
    public User findByIdForUpdate(Long id) {
        return userRepository.lockForUpdate()
            .load(QUser.user.id.eq(id))
            .orElse(null);
    }
    
    // 包含已删除数据
    public List<User> findAllIncludingDeleted() {
        return userRepository.withDeleted().loads();
    }
    
    // 只查询已删除数据
    public List<User> findDeleted() {
        return userRepository.onlyDeleted().loads();
    }
    
    // 计数
    public long countByStatus(UserStatus status) {
        return userRepository.count(QUser.user.status.eq(status));
    }
    
    // 存在性检查
    public boolean existsByUsername(String username) {
        return userRepository.exists(QUser.user.username.eq(username));
    }
    
    // 删除（软删除）
    public void deleteById(Long id) {
        userRepository.delete(QUser.user.id.eq(id));
    }
    
    // 物理删除
    public void physicalDeleteById(Long id) {
        userRepository.physicalDelete().delete(QUser.user.id.eq(id));
    }
}
```

### 5. 使用 DSL Query 参数

```java
// 定义查询参数类（继承 DslQuery）
public class UserQuery extends DslQuery<User> {
    
    @Condition(field = "username")
    private String username;
    
    @Condition(field = "status")
    private UserStatus status;
    
    @Condition(type = QueryType.GTE, field = "createdAt")
    private LocalDateTime startDate;
    
    @Condition(type = QueryType.LTE, field = "createdAt")
    private LocalDateTime endDate;
    
    @ConditionIgnore
    private String sortStr;  // 不参与查询
    
    @ConditionIgnore
    private Integer offset;
    
    @ConditionIgnore
    private Integer limit;
}

// 使用
public Page<User> query(UserQuery query) {
    return userRepository.page(query);
}
```

### 6. QueryDSL 使用

```java
// Q 类由 QueryDSL 自动生成
import com.dev.lib.jpa.entity.QUser;

// 静态导入
import static com.dev.lib.jpa.entity.QUser.user;

// 使用
List<User> users = userRepository.loads(
    user.status.eq(UserStatus.ACTIVE)
    .and(user.age.goe(18))
);
```

## 面向 LLM

### 核心组件

1. **JpaEntity**：实体基类
   - 包含 id、status、createdAt、updatedAt、creatorId、modifierId

2. **BaseRepository**：Repository 基类
   - 继承 JpaRepository 和 ListQuerydslPredicateExecutor
   - 提供 QueryBuilder 链式查询
   - 自动处理软删除

3. **QueryBuilder**：链式查询构建器
   - 上下文配置：`lockForUpdate()`, `lockForShare()`, `withDeleted()`, `onlyDeleted()`
   - 字段选择：`select(SFunction...)`, `select(String...)`
   - 终结操作：`load()`, `loads()`, `page()`, `stream()`, `count()`, `exists()`, `delete()`

4. **DslQuery**：查询参数基类
   - `@Condition`：标记查询字段
   - `@ConditionIgnore`：标记非查询字段
   - 自动支持分页和排序

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-data-jpa/src/main/java/com/dev/lib/jpa/`
- Git 提交：使用 `git log --oneline common-data-jpa/` 查看
