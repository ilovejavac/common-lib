# COW 重构迁移检查清单

## ✅ 已完成的重构

- [x] 创建 `SysFileCowService` 统一写入服务
- [x] 添加 COW 配置开关 `storage.vfs.cow-enabled`
- [x] 重构 `VfsVersionManager` 使用统一服务
- [x] 重构 `AbstractChainStorage` 使用统一服务
- [x] 添加 `addOldVersion()` 方法供 Storage 使用

## 🔧 需要手动完成的步骤

### 1. 更新 AbstractChainStorage 的子类

需要在构造函数中添加 `SysFileCowService` 参数：

#### MinioChainStorage
```java
// 文件位置：common-storage/src/main/java/com/dev/lib/storage/domain/service/chain/MinioChainStorage.java

public MinioChainStorage(
    AppStorageProperties fileProperties,
    VfsPathRepository fileRepository,
    StorageServiceNameProvider serviceNameProvider,
    SysFileCowService sysFileWriteService,  // ← 添加这个参数
    // ... 其他依赖
) {
    super(fileProperties, fileRepository, serviceNameProvider, sysFileWriteService);  // ← 传递给父类
    // ...
}
```

#### OssChainStorage
```java
// 文件位置：common-storage/src/main/java/com/dev/lib/storage/domain/service/chain/OssChainStorage.java

public OssChainStorage(
    AppStorageProperties fileProperties,
    VfsPathRepository fileRepository,
    StorageServiceNameProvider serviceNameProvider,
    SysFileCowService sysFileWriteService,  // ← 添加这个参数
    // ... 其他依赖
) {
    super(fileProperties, fileRepository, serviceNameProvider, sysFileWriteService);  // ← 传递给父类
    // ...
}
```

#### LocalChainStorage
```java
// 文件位置：common-storage/src/main/java/com/dev/lib/storage/domain/service/chain/LocalChainStorage.java

public LocalChainStorage(
    AppStorageProperties fileProperties,
    VfsPathRepository fileRepository,
    StorageServiceNameProvider serviceNameProvider,
    SysFileCowService sysFileWriteService,  // ← 添加这个参数
    // ... 其他依赖
) {
    super(fileProperties, fileRepository, serviceNameProvider, sysFileWriteService);  // ← 传递给父类
    // ...
}
```

### 2. 编译验证

```bash
cd /Users/lucheng/project/dev/common-lib
mvn clean compile
```

### 3. 运行测试

```bash
mvn test
```

### 4. 配置文件（可选）

如果需要禁用 COW，在 `application.yml` 中添加：

```yaml
storage:
  vfs:
    cow-enabled: false  # 禁用 COW，直接覆盖
```

## 🧪 测试场景

### 场景 1：VFS 和 Storage 交替写入

```java
// VFS 写入
Vfs.root("/bucket").file("test.txt").write("v1");

// Storage 写入
Storage.bucket("bucket").object("test.txt").write("v2");

// VFS 再次写入
Vfs.root("/bucket").file("test.txt").write("v3");

// 验证：oldStoragePaths 应该包含 v1 和 v2 的路径
```

### 场景 2：禁用 COW

```yaml
storage:
  vfs:
    cow-enabled: false
```

```java
// 写入后，oldStoragePaths 应该为空
Vfs.root("/bucket").file("test.txt").write("v1");
Vfs.root("/bucket").file("test.txt").write("v2");

SysFile file = ...;
assertNull(file.getOldStoragePaths());
```

### 场景 3：版本清理

```java
// 写入 15 次，触发版本清理
for (int i = 0; i < 15; i++) {
    Vfs.root("/bucket").file("test.txt").write("v" + i);
}

// 验证：oldStoragePaths 最多保留 10 个
SysFile file = ...;
assertTrue(file.getOldStoragePaths().size() <= 10);
```

## 📝 注意事项

1. **乐观锁冲突**：高并发写入可能触发 `OptimisticLockException`
   - 解决方案：使用悲观锁 `findByVirtualPathForUpdate()`

2. **延迟删除**：旧版本文件 5 分钟后才会被清理
   - 短时间内存储空间会增加

3. **性能影响**：COW 会有轻微性能开销
   - 如果不需要版本管理，可以禁用 COW

## 🎯 验收标准

- [ ] 编译通过，无错误
- [ ] 单元测试通过
- [ ] VFS 和 Storage 交替写入，版本链完整
- [ ] 禁用 COW 后，不保留旧版本
- [ ] 版本数量超过 10 个时，自动清理最老的 5 个

## 📚 相关文档

- [COW_REFACTORING.md](./COW_REFACTORING.md) - 详细重构说明
- [BASH_USAGE.md](../common-bash/BASH_USAGE.md) - Bash 模块使用指南
