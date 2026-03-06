# COW 机制重构说明

## 重构目标

解决 Storage 和 VFS 并发写入同一文件时的数据不一致和文件丢失问题，统一 COW（Copy-On-Write）机制。

---

## 问题分析

### 重构前的问题

```
时刻 T1: VFS 写入（COW）
  storagePath: path1
  oldStoragePaths: []

时刻 T2: VFS 再次写入（COW）
  storagePath: path2
  oldStoragePaths: [path1]  ← 保留旧版本

时刻 T3: Storage 写入（直接覆盖）
  storagePath: path3  ← 直接覆盖
  oldStoragePaths: [path1]  ← path2 丢失！❌
```

**根本原因**：
- VFS 使用 `VfsVersionManager.writeWithCOW()` 实现 COW
- Storage 使用 `AbstractChainStorage.saveFileRecord()` 直接覆盖
- 两者操作同一个 `SysFile` 实体，但逻辑不一致

---

## 重构方案

### 架构设计

```
┌─────────────────────────────────────────────────────┐
│         统一写入层（COW 逻辑）                         │
│      SysFileCowService (新增)                      │
│  - writeWithCOW()                                    │
│  - appendWithCOW()                                   │
│  - addOldVersion()                                   │
│  - 配置开关: storage.vfs.cow-enabled                  │
└─────────────────────────────────────────────────────┘
                    ↑                ↑
                    │                │
        ┌───────────┘                └───────────┐
        │                                        │
┌───────────────┐                      ┌─────────────────┐
│  Storage API  │                      │    VFS API      │
│AbstractChainStorage                  │VfsVersionManager│
│  (通过 bizId)  │                      │ (通过 virtualPath)│
└───────────────┘                      └─────────────────┘
        ↓                                        ↓
┌───────────────┐                      ┌─────────────────┐
│SysFileBizIdRepository│                    │VfsPathRepository│
│  (bizId 查询)  │                      │ (virtualPath 查询)│
└───────────────┘                      └─────────────────┘
                    ↓                ↓
            ┌─────────────────────────┐
            │      SysFile 实体        │
            │  - storagePath           │
            │  - oldStoragePaths       │
            │  - version (乐观锁)       │
            └─────────────────────────┘
```

### 核心变更

#### 1. 新增 `SysFileCowService`

**位置**：`com.dev.lib.storage.domain.service.write.SysFileCowService`

**职责**：
- 统一 COW 写入逻辑
- 版本管理（保留最多 10 个旧版本）
- 延迟删除（5 分钟后异步清理）
- 支持配置开关

**核心方法**：
```java
// 写入文件（带 COW）
String writeWithCOW(SysFile file, InputStream contentStream, long size, String fileName)

// 追加内容（带 COW）
String appendWithCOW(SysFile file, byte[] contentBytes, String fileName)

// 手动添加旧版本（供 Storage 使用）
void addOldVersion(SysFile file, String oldStoragePath)

// 清理旧版本
void cleanupOldVersions(SysFile file)
```

#### 2. 重构 `VfsVersionManager`

**变更**：
- 移除内部的 COW 逻辑（`manageOldVersions` 等）
- 委托给 `SysFileCowService`

**示例**：
```java
// 重构前
public void writeWithCOW(...) {
    String oldStoragePath = file.getStoragePath();
    String newStoragePath = storageService.upload(...);
    updateFileWithVersioning(file, newStoragePath, size, oldStoragePath);
    // 内部管理旧版本
}

// 重构后
public void writeWithCOW(...) {
    String fileName = pathResolver.getName(fullPath);
    sysFileWriteService.writeWithCOW(file, contentStream, size, fileName);
    // VFS 特定逻辑
    applyTemporaryMetadataForUpdate(file, ctx);
    fileRepository.save(file);
}
```

#### 3. 重构 `AbstractChainStorage`

**变更**：
- 添加 `SysFileCowService` 依赖
- `saveFileRecord()` 方法使用 COW 逻辑

**示例**：
```java
// 重构前
protected String saveFileRecord(...) {
    if (existing.isPresent()) {
        file.setStoragePath(storagePath);  // 直接覆盖 ❌
        file.setSize(size);
    }
}

// 重构后
protected String saveFileRecord(...) {
    if (existing.isPresent()) {
        applyCOWForExistingFile(file, storagePath, size);  // 使用 COW ✅
    }
}

private void applyCOWForExistingFile(SysFile file, String newStoragePath, long newSize) {
    String oldStoragePath = file.getStoragePath();
    if (oldStoragePath != null && !oldStoragePath.equals(newStoragePath)) {
        sysFileWriteService.addOldVersion(file, oldStoragePath);  // 保存旧版本
    }
    file.setStoragePath(newStoragePath);
    file.setSize(newSize);
}
```

#### 4. 配置开关

**位置**：`application.yml`

```yaml
storage:
  vfs:
    # 是否启用 COW（默认 true）
    cow-enabled: true

    # 临时文件过期时间（分钟）
    temporary-ttl-minutes: 60
```

**说明**：
- `cow-enabled: true`（默认）：启用 COW，保留旧版本
- `cow-enabled: false`：禁用 COW，直接覆盖（性能优化）

---

## Repository 分析

### 为什么不合并 Repository？

| Repository | 查询维度 | 使用场景 | 原因 |
|-----------|---------|---------|------|
| **SysFileBizIdRepository** | bizId | Storage API | 用户通过 bizId 定位文件 |
| **VfsPathRepository** | virtualPath | VFS 文件系统 | 用户通过虚拟路径定位文件 |
| **VfsFileRepository** | 封装 VfsPathRepository | VFS 领域层 | 自动注入 serviceName |

**结论**：
- 两个 Repository 查询维度不同，不应合并
- 但写入逻辑统一到 `SysFileCowService`

---

## 重构后的效果

### 并发写入场景

```java
// 时刻 T1: VFS 写入（COW）
Vfs.root("/bucket").file("data.txt").write("v1");
// storagePath: path1
// oldStoragePaths: []

// 时刻 T2: VFS 再次写入（COW）
Vfs.root("/bucket").file("data.txt").write("v2");
// storagePath: path2
// oldStoragePaths: [path1]  ← 保留旧版本

// 时刻 T3: Storage 写入（COW）✅
Storage.bucket("bucket").object("data.txt").write("v3");
// storagePath: path3
// oldStoragePaths: [path1, path2]  ← path2 也被保留！✅
```

### 性能对比

| 场景 | COW 启用 | COW 禁用 |
|------|---------|---------|
| 写入性能 | 正常 | 略快（无版本管理开销）|
| 存储空间 | 较高（保留 10 个版本）| 低（单版本）|
| 并发安全 | ✅ 安全 | ⚠️ 可能读到不一致状态 |
| 版本回滚 | ✅ 支持 | ❌ 不支持 |

---

## 迁移指南

### 1. 更新依赖注入

**VfsVersionManager**：
```java
// 添加依赖
private final SysFileCowService sysFileWriteService;
```

**AbstractChainStorage**：
```java
// 添加依赖
protected final SysFileCowService sysFileWriteService;
```

**子类（MinioChainStorage, OssChainStorage, LocalChainStorage）**：
```java
// 构造函数添加参数
public MinioChainStorage(
    AppStorageProperties fileProperties,
    VfsPathRepository fileRepository,
    StorageServiceNameProvider serviceNameProvider,
    SysFileCowService sysFileWriteService,  // 新增
    // ... 其他依赖
) {
    super(fileProperties, fileRepository, serviceNameProvider, sysFileWriteService);
    // ...
}
```

### 2. 配置文件（可选）

```yaml
storage:
  vfs:
    # 禁用 COW（如果不需要版本管理）
    cow-enabled: false
```

### 3. 测试验证

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

---

## 注意事项

### 1. 乐观锁冲突

`SysFile` 实体有 `@Version` 字段，高并发写入可能触发乐观锁冲突。

**解决方案**：
- 使用悲观锁：`findByVirtualPathForUpdate()`
- 重试机制：捕获 `OptimisticLockException` 并重试

### 2. 延迟删除

旧版本文件会在 5 分钟后异步清理，短时间内存储空间会增加。

**解决方案**：
- 调整 `DELAY_DELETE_MINUTES` 常量
- 手动调用 `sysFileWriteService.cleanupOldVersions(file)`

### 3. 性能优化

如果不需要版本管理，可以禁用 COW：

```yaml
storage:
  vfs:
    cow-enabled: false
```

---

## 总结

| 维度 | 重构前 | 重构后 |
|------|--------|--------|
| **COW 一致性** | ❌ VFS 有，Storage 无 | ✅ 统一 COW 逻辑 |
| **并发安全** | ❌ 文件丢失风险 | ✅ 版本链完整 |
| **代码复用** | ❌ 重复逻辑 | ✅ 统一服务 |
| **配置灵活性** | ❌ 无法关闭 COW | ✅ 支持开关 |
| **Repository** | ✅ 查询维度清晰 | ✅ 保持不变 |

**核心收益**：
1. ✅ 解决 Storage 和 VFS 并发写入的文件丢失问题
2. ✅ 统一 COW 逻辑，减少代码重复
3. ✅ 支持配置开关，灵活控制性能和功能
4. ✅ 保持 Repository 查询维度清晰，不强行合并
