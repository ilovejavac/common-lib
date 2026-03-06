# Repository 合并可行性分析

## 📊 当前状态

### VfsPathRepository
```java
public interface VfsPathRepository extends BaseRepository<SysFile> {
    // 查询维度：virtualPath + serviceName
    Optional<SysFile> findByVirtualPath(String serviceName, String virtualPath);
    Optional<SysFile> findByVirtualPathForUpdate(String serviceName, String virtualPath);
    List<SysFile> findByParentPath(String serviceName, String parentPath);
    List<SysFile> findByVirtualPathStartingWith(String serviceName, String prefix);
    List<SysFile> findByStoragePath(String serviceName, String storagePath);
}
```

**使用场景**：
- ✅ AbstractChainStorage（Storage API）
- ✅ VfsFileRepository（VFS 领域层）

### SysFileBizIdRepository
```java
public interface SysFileBizIdRepository extends BaseRepository<SysFile> {
    // 查询维度：bizId
    Optional<SysFile> findByBizId(String bizId);
    Optional<SysFile> findByBizIdForUpdate(String bizId);
    List<SysFile> findAllByBizIdIn(Collection<String> bizIds);
    Optional<SysFile> findByBizIdAndServiceName(String bizId, String serviceName);

    // 清理任务专用
    List<SysFile> loads(Query query);  // 支持 deleteAfterLe, expirationAtLe, temporary
}
```

**使用场景**：
- ✅ VfsCleanupTask（定时清理任务）
- ✅ VfsAsyncCleanupService（异步清理）
- ✅ StorageFileAdapt（适配器层）

---

## 🔍 深度分析

### 1. 查询维度对比

| Repository | 主键查询 | 路径查询 | 清理查询 |
|-----------|---------|---------|---------|
| **VfsPathRepository** | ❌ | ✅ virtualPath | ❌ |
| **SysFileBizIdRepository** | ✅ bizId | ❌ | ✅ deleteAfter, expiration |

**结论**：查询维度**完全不同**，没有重叠。

### 2. 使用场景对比

#### VfsPathRepository 使用场景
```java
// 场景 1：Storage API 通过 virtualPath 查询
AbstractChainStorage:
  fileRepository.findByVirtualPath(serviceName, bucketName + "/" + objectKey)

// 场景 2：VFS 通过 virtualPath 查询
VfsFileRepository:
  fileSystemRepository.findByVirtualPath(currentService(), virtualPath)
  fileSystemRepository.findByParentPath(currentService(), parentPath)
  fileSystemRepository.findByVirtualPathStartingWith(currentService(), prefix)
```

#### SysFileBizIdRepository 使用场景
```java
// 场景 1：定时清理任务
VfsCleanupTask:
  sysFileRepository.loads(new Query()
    .setDeleteAfterLe(now)
    .setServiceName(serviceName))

// 场景 2：异步清理
VfsAsyncCleanupService:
  sysFileRepository.loads(new Query()
    .setExpirationAtLe(now)
    .setTemporary(true))

// 场景 3：适配器层（通过 bizId 查询）
StorageFileAdapt:
  fileRepository.findByBizId(bizId)
  fileRepository.findAllByBizIdIn(bizIds)
```

**结论**：使用场景**完全不同**，职责清晰。

---

## ✅ 可以合并吗？

### 答案：**可以合并，但不推荐**

### 合并方案

```java
public interface SysFileBizIdRepository extends BaseRepository<SysFile> {

    // ==================== bizId 查询（原 SysFileBizIdRepository）====================

    Optional<SysFile> findByBizId(String bizId);
    Optional<SysFile> findByBizIdForUpdate(String bizId);
    List<SysFile> findAllByBizIdIn(Collection<String> bizIds);

    // ==================== virtualPath 查询（原 VfsPathRepository）====================

    default Optional<SysFile> findByVirtualPath(String serviceName, String virtualPath) {
        return load(new Query().setServiceName(serviceName).setVirtualPath(virtualPath));
    }

    default Optional<SysFile> findByVirtualPathForUpdate(String serviceName, String virtualPath) {
        return lockForUpdate().load(new Query().setServiceName(serviceName).setVirtualPath(virtualPath));
    }

    default List<SysFile> findByParentPath(String serviceName, String parentPath) {
        return loads(new Query().setServiceName(serviceName).setParentPath(parentPath));
    }

    default List<SysFile> findByVirtualPathStartingWith(String serviceName, String prefix) {
        return loads(new Query().setServiceName(serviceName).setVirtualPathStartWith(prefix));
    }

    // ==================== 清理任务查询 ====================

    @Data
    class Query extends DslQuery<SysFile> {
        private String bizId;
        private Collection<String> bizIdIn;
        private String serviceName;
        private String virtualPath;
        private String parentPath;
        private String virtualPathStartWith;
        private Collection<String> virtualPathIn;
        private String storagePath;
        private LocalDateTime deleteAfterLe;
        private LocalDateTime expirationAtLe;
        private Boolean temporary;
        @Condition(field = "temporary", operator = LogicalOperator.OR)
        private Boolean orTemporary;
    }
}
```

### 合并后的影响

#### ✅ 优点
1. **减少接口数量**：从 2 个减少到 1 个
2. **统一入口**：所有查询都通过一个 Repository
3. **简化依赖注入**：只需注入一个 Repository

#### ❌ 缺点
1. **职责不清晰**：一个 Repository 承担了多种查询职责
2. **Query 类膨胀**：Query 类包含了所有查询字段，容易混淆
3. **违反 ISP（接口隔离原则）**：
   - Storage API 只需要 virtualPath 查询，但会看到 bizId 查询
   - 清理任务只需要 deleteAfter 查询，但会看到 virtualPath 查询
4. **命名混乱**：
   - 如果叫 `SysFileBizIdRepository`，不能体现 virtualPath 查询
   - 如果叫 `VfsPathRepository`，不能体现 bizId 查询
5. **破坏领域边界**：
   - VFS 领域不应该知道 bizId 的存在
   - Storage API 不应该知道 parentPath 的存在

---

## 🎯 推荐方案：**保持分离**

### 理由

#### 1. 符合单一职责原则（SRP）

```
VfsPathRepository：文件系统查询（virtualPath 维度）
SysFileBizIdRepository：业务查询（bizId 维度）+ 清理任务查询
```

#### 2. 符合接口隔离原则（ISP）

```
VFS 领域 → VfsFileRepository → VfsPathRepository
         只看到 virtualPath 查询 ✅

Storage API → AbstractChainStorage → VfsPathRepository
            只看到 virtualPath 查询 ✅

清理任务 → VfsCleanupTask → SysFileBizIdRepository
         只看到 deleteAfter 查询 ✅

适配器层 → StorageFileAdapt → SysFileBizIdRepository
         只看到 bizId 查询 ✅
```

#### 3. 领域边界清晰

```
┌─────────────────────────────────────┐
│         VFS 领域                     │
│  - virtualPath 是核心概念             │
│  - 不关心 bizId                       │
│  - 使用 VfsPathRepository         │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       Storage API 领域               │
│  - virtualPath 是查询维度             │
│  - 使用 VfsPathRepository         │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       清理任务 / 适配器层             │
│  - bizId 是核心概念                   │
│  - deleteAfter, expiration 是查询维度 │
│  - 使用 SysFileBizIdRepository            │
└─────────────────────────────────────┘
```

#### 4. 查询性能优化

```java
// VfsPathRepository 的索引
@Index(name = "idx_service_virtual_path", columnList = "serviceName,virtualPath")
@Index(name = "idx_parent_path", columnList = "parentPath")

// SysFileBizIdRepository 的索引
@Index(name = "idx_biz_id", columnList = "bizId")
@Index(name = "idx_delete_after", columnList = "deleteAfter")
@Index(name = "idx_expiration_at", columnList = "expirationAt")
```

不同的查询维度需要不同的索引优化。

---

## 🔄 如果一定要合并

### 方案 1：合并到 SysFileBizIdRepository（推荐）

```java
public interface SysFileBizIdRepository extends BaseRepository<SysFile> {

    // 保留所有现有方法
    // ...

    // 添加 VfsPathRepository 的方法
    default Optional<SysFile> findByVirtualPath(String serviceName, String virtualPath) {
        return load(new Query().setServiceName(serviceName).setVirtualPath(virtualPath));
    }

    // ... 其他方法
}
```

**步骤**：
1. 将 VfsPathRepository 的所有方法复制到 SysFileBizIdRepository
2. 更新 Query 类，添加 virtualPath 相关字段
3. 删除 VfsPathRepository 接口
4. 更新所有使用 VfsPathRepository 的地方

### 方案 2：创建统一的 SysFileUnifiedRepository

```java
public interface SysFileUnifiedRepository extends BaseRepository<SysFile> {
    // 合并所有方法
}

// 保留原有接口作为视图
public interface VfsPathRepository extends SysFileUnifiedRepository {
    // 只暴露 virtualPath 查询
}

public interface SysFileBizIdRepository extends SysFileUnifiedRepository {
    // 只暴露 bizId 查询
}
```

**优点**：保持接口隔离，但实现统一。

---

## 📝 最终建议

### ✅ 推荐：**保持分离**

**理由**：
1. 职责清晰，符合 SRP 和 ISP
2. 领域边界明确
3. 查询性能优化独立
4. 代码可读性更好

### ⚠️ 如果一定要合并

选择**方案 1**（合并到 SysFileBizIdRepository），因为：
1. 实现简单
2. 减少接口数量
3. 统一入口

但需要注意：
- Query 类会变得很大
- 需要良好的文档说明不同查询的用途
- 可能违反接口隔离原则

---

## 🎯 结论

**当前设计是合理的**，两个 Repository 职责清晰，查询维度不同，使用场景不同。

**不建议合并**，除非有明确的业务需求（如统一查询入口、减少依赖注入等）。

如果合并，建议使用**方案 1**（合并到 SysFileBizIdRepository），但要做好文档和注释。
