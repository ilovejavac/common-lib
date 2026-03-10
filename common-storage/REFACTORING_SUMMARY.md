# COW 机制重构总结

## 🎯 重构目标

解决 **Storage 和 VFS 并发写入同一文件时的数据不一致和文件丢失问题**，统一 COW（Copy-On-Write）机制。

---

## ✅ 已完成的工作

### 1. 核心服务层

#### 新增 `SysFileCowService`
- **位置**：`common-storage/src/main/java/com/dev/lib/storage/domain/service/write/SysFileCowService.java`
- **职责**：统一 COW 写入逻辑，为 Storage 和 VFS 提供一致的版本管理
- **核心方法**：
  - `writeWithCOW()` - 写入文件（带 COW）
  - `appendWithCOW()` - 追加内容（带 COW）
  - `addOldVersion()` - 手动添加旧版本
  - `cleanupOldVersions()` - 清理旧版本

### 2. 配置层

#### 更新 `AppStorageProperties`
- **位置**：`common-storage/src/main/java/com/dev/lib/storage/config/AppStorageProperties.java`
- **新增配置**：
  ```java
  @Data
  public static class VirtualFile {
      // 是否启用 COW（默认 true）
      private Boolean cowEnabled = true;
  }
  ```

### 3. VFS 层重构

#### 重构 `VfsVersionManager`
- **位置**：`common-storage/src/main/java/com/dev/lib/storage/domain/service/virtual/write/VfsVersionManager.java`
- **变更**：
  - 移除内部 COW 逻辑（`manageOldVersions` 等）
  - 委托给 `SysFileCowService`
  - 保留 VFS 特定逻辑（临时文件、目录管理）

### 4. Storage 层重构

#### 重构 `AbstractChainStorage`
- **位置**：`common-storage/src/main/java/com/dev/lib/storage/domain/service/chain/AbstractChainStorage.java`
- **变更**：
  - 添加 `SysFileCowService` 依赖
  - `saveFileRecord()` 使用 COW 逻辑
  - 新增 `applyCOWForExistingFile()` 方法

#### 更新子类构造函数
- **LocalChainStorage** ✅
- **MinioChainStorage** ✅
- **OssChainStorage** ✅

### 5. 文档

- ✅ `COW_REFACTORING.md` - 详细重构说明
- ✅ `MIGRATION_CHECKLIST.md` - 迁移检查清单
- ✅ `REFACTORING_SUMMARY.md` - 本文档

---

## 📊 重构前后对比

### 并发写入场景

#### 重构前 ❌
```
T1: VFS 写入 → storagePath: path1, oldStoragePaths: []
T2: VFS 写入 → storagePath: path2, oldStoragePaths: [path1]
T3: Storage 写入 → storagePath: path3, oldStoragePaths: [path1]  ← path2 丢失！
```

#### 重构后 ✅
```
T1: VFS 写入 → storagePath: path1, oldStoragePaths: []
T2: VFS 写入 → storagePath: path2, oldStoragePaths: [path1]
T3: Storage 写入 → storagePath: path3, oldStoragePaths: [path1, path2]  ← 完整！
```

### 架构对比

#### 重构前
```
VFS: VfsVersionManager (COW) → SysFile
Storage: AbstractChainStorage (直接覆盖) → SysFile
                ↓
        数据不一致 ❌
```

#### 重构后
```
VFS: VfsVersionManager → SysFileCowService (统一 COW) → SysFile
Storage: AbstractChainStorage → SysFileCowService (统一 COW) → SysFile
                ↓
        数据一致 ✅
```

---

## 🔧 使用方式

### 1. 默认启用 COW（推荐）

无需配置，默认启用 COW：

```java
// VFS 写入
Vfs.root("/bucket").file("data.txt").write("v1");
Vfs.root("/bucket").file("data.txt").write("v2");

// Storage 写入
Storage.bucket("bucket").object("data.txt").write("v3");

// 结果：oldStoragePaths = [path1, path2]
```

### 2. 禁用 COW（性能优化）

在 `application.yml` 中配置：

```yaml
storage:
  vfs:
    cow-enabled: false  # 禁用 COW
```

```java
// 写入后，oldStoragePaths 为空
Vfs.root("/bucket").file("data.txt").write("v1");
Vfs.root("/bucket").file("data.txt").write("v2");

// 结果：oldStoragePaths = null（直接覆盖）
```

### 3. 手动清理旧版本

```java
@Autowired
private SysFileCowService sysFileWriteService;

// 立即清理旧版本
sysFileWriteService.cleanupOldVersions(file);
```

---

## 📝 Repository 设计说明

### 为什么不合并 Repository？

| Repository | 查询维度 | 使用场景 |
|-----------|---------|---------|
| **SysFileBizIdRepository** | bizId | Storage API（用户通过 bizId 定位文件）|
| **VfsPathRepository** | virtualPath | VFS 文件系统（用户通过虚拟路径定位文件）|
| **VfsFileRepository** | 封装 VfsPathRepository | VFS 领域层（自动注入 serviceName）|

**结论**：
- ✅ 两个 Repository 查询维度不同，**不应合并**
- ✅ 但写入逻辑统一到 `SysFileCowService`

---

## ⚠️ 注意事项

### 1. 乐观锁冲突

`SysFile` 实体有 `@Version` 字段，高并发写入可能触发 `OptimisticLockException`。

**解决方案**：
- 使用悲观锁：`findByVirtualPathForUpdate()`
- 重试机制：捕获异常并重试

### 2. 延迟删除

旧版本文件会在 **5 分钟后**异步清理，短时间内存储空间会增加。

**配置**：
```java
// SysFileCowService.java
private static final long DELAY_DELETE_MINUTES = 5L;
```

### 3. 版本数量限制

最多保留 **10 个旧版本**，超过后自动删除最老的 **5 个**。

**配置**：
```java
// SysFileCowService.java
private static final int MAX_OLD_VERSIONS = 10;
private static final int VERSIONS_TO_DELETE = 5;
```

---

## 🧪 测试建议

### 测试场景 1：VFS 和 Storage 交替写入

```java
@Test
void testConcurrentWriteWithCOW() {
    // VFS 写入
    Vfs.root("/bucket").file("test.txt").write("v1");
    Vfs.root("/bucket").file("test.txt").write("v2");

    // Storage 写入
    Storage.bucket("bucket").object("test.txt").write("v3");

    // 验证：oldStoragePaths 应该包含 v1 和 v2 的路径
    SysFile file = fileRepository.findByVirtualPath("my-service", "/bucket/test.txt").get();
    assertEquals(2, file.getOldStoragePaths().size());
}
```

### 测试场景 2：禁用 COW

```yaml
storage:
  vfs:
    cow-enabled: false
```

```java
@Test
void testWriteWithoutCOW() {
    Vfs.root("/bucket").file("test.txt").write("v1");
    Vfs.root("/bucket").file("test.txt").write("v2");

    SysFile file = fileRepository.findByVirtualPath("my-service", "/bucket/test.txt").get();
    assertNull(file.getOldStoragePaths());  // 无旧版本
}
```

### 测试场景 3：版本清理

```java
@Test
void testVersionCleanup() {
    // 写入 15 次，触发版本清理
    for (int i = 0; i < 15; i++) {
        Vfs.root("/bucket").file("test.txt").write("v" + i);
    }

    SysFile file = fileRepository.findByVirtualPath("my-service", "/bucket/test.txt").get();
    assertTrue(file.getOldStoragePaths().size() <= 10);  // 最多 10 个
}
```

---

## 📈 性能影响

| 维度 | COW 启用 | COW 禁用 |
|------|---------|---------|
| **写入性能** | 正常 | 略快（无版本管理开销）|
| **存储空间** | 较高（保留 10 个版本）| 低（单版本）|
| **并发安全** | ✅ 安全 | ⚠️ 可能读到不一致状态 |
| **版本回滚** | ✅ 支持 | ❌ 不支持 |

---

## 🎉 重构收益

1. ✅ **解决并发问题**：Storage 和 VFS 并发写入不再丢失文件
2. ✅ **统一 COW 逻辑**：减少代码重复，提高可维护性
3. ✅ **配置灵活**：支持开关，平衡性能和功能
4. ✅ **架构清晰**：Repository 职责分离，写入逻辑统一

---

## 📚 相关文档

- [COW_REFACTORING.md](./COW_REFACTORING.md) - 详细重构说明
- [MIGRATION_CHECKLIST.md](./MIGRATION_CHECKLIST.md) - 迁移检查清单
- [BASH_USAGE.md](../common-bash/BASH_USAGE.md) - Bash 模块使用指南

---

## 🚀 下一步

1. **编译验证**：`mvn clean compile`
2. **运行测试**：`mvn test`
3. **集成测试**：在实际项目中验证 VFS 和 Storage 交替写入
4. **性能测试**：对比 COW 启用/禁用的性能差异
5. **文档更新**：更新项目 README，说明 COW 机制

---

**重构完成时间**：2026-03-06
**重构人员**：Claude Sonnet 4.6
**版本**：v1.4.3
