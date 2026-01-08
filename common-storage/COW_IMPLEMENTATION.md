# VFS Copy-on-Write 实现说明

## 📖 核心原理

### 传统方式的问题
```
写操作：
1. 加锁
2. 删除旧文件
3. 上传新文件  ← 此时读操作会失败！
4. 更新数据库
5. 释放锁

问题：写操作期间，读操作会读取失败
```

### Copy-on-Write 方式
```
写操作：
1. 上传新文件到新路径（不影响旧文件）
2. 加锁（只锁定数据库更新）
3. 原子性地更新 storagePath 指针
4. 标记旧文件延迟删除
5. 释放锁

读操作：
- 始终读取当前的 storagePath
- 写操作期间读取的是旧版本
- 写操作完成后读取的是新版本
- 不需要加锁！
```

## 🔄 时间线示例

```
时刻 T0: 文件 /test.txt 存在
  - storagePath = "vfs/2026/01/08/abc123.txt"
  - oldStoragePath = null

时刻 T1: 线程 A 开始读取
  - 读取 storagePath = "vfs/2026/01/08/abc123.txt"
  - 下载文件内容

时刻 T2: 线程 B 开始写入（同时进行）
  - 上传新文件到 "vfs/2026/01/08/def456.txt"
  - 此时线程 A 仍在读取旧文件，不受影响

时刻 T3: 线程 B 更新数据库（原子操作）
  - storagePath = "vfs/2026/01/08/def456.txt"
  - oldStoragePath = "vfs/2026/01/08/abc123.txt"
  - deleteAfter = T3 + 5分钟

时刻 T4: 线程 A 读取完成
  - 成功读取到旧版本内容

时刻 T5: 线程 C 开始读取
  - 读取 storagePath = "vfs/2026/01/08/def456.txt"
  - 读取到新版本内容

时刻 T8 (T3 + 5分钟): 清理任务执行
  - 删除 "vfs/2026/01/08/abc123.txt"
  - 清除 oldStoragePath 和 deleteAfter 标记
```

## 🎯 关键优势

### 1. 读写不互斥
- **读操作**：无锁，直接读取当前版本
- **写操作**：只在更新指针时短暂加锁
- **并发性能**：读操作完全不受写操作影响

### 2. 数据一致性
- 指针更新是原子操作（数据库事务保证）
- 读操作要么看到旧版本，要么看到新版本
- 不会看到中间状态

### 3. 崩溃恢复
```
场景 1: 上传新文件时崩溃
  - 数据库未更新，仍指向旧文件
  - 新文件成为孤儿文件（可通过清理任务删除）
  - 系统状态一致

场景 2: 更新指针后崩溃
  - 数据库已更新，指向新文件
  - 旧文件标记为待删除
  - 清理任务会在 5 分钟后删除旧文件
  - 系统状态一致
```

## 📊 并发场景分析

### 场景 1: 多个读操作并发
```java
// 线程 1
String content1 = vfs.read(ctx, "/test.txt");  // 无锁

// 线程 2
String content2 = vfs.read(ctx, "/test.txt");  // 无锁

// 线程 3
String content3 = vfs.read(ctx, "/test.txt");  // 无锁

结果：完全并发，性能最优
```

### 场景 2: 读写并发
```java
// 线程 1: 读操作
String content = vfs.read(ctx, "/test.txt");
// 读取旧版本 "vfs/.../abc123.txt"

// 线程 2: 写操作（同时进行）
vfs.write(ctx, "/test.txt", "new content");
// 1. 上传到 "vfs/.../def456.txt"
// 2. 更新指针（短暂加锁）

结果：
- 线程 1 读取到旧版本（一致性保证）
- 线程 2 写入成功
- 互不影响
```

### 场景 3: 多个写操作并发
```java
// 线程 1
vfs.write(ctx, "/test.txt", "content 1");
// 获取悲观锁 → 上传 → 更新指针 → 释放锁

// 线程 2
vfs.write(ctx, "/test.txt", "content 2");
// 等待锁 → 获取锁 → 上传 → 更新指针 → 释放锁

结果：
- 串行化执行（悲观锁保证）
- 最终内容为 "content 2"
```

## 🧹 清理机制

### 延迟删除策略
```java
// 写操作时标记
file.setOldStoragePath("vfs/.../old.txt");
file.setDeleteAfter(LocalDateTime.now().plusMinutes(5));

// 5 分钟后，定时任务清理
@Scheduled(fixedRate = 300000)
public void cleanupOldFiles() {
    // 查询 deleteAfter < now 的记录
    // 删除 oldStoragePath 指向的文件
    // 清除标记
}
```

### 为什么延迟 5 分钟？
1. **保护正在读取的操作**：给足够时间完成读取
2. **网络延迟容忍**：考虑慢速网络连接
3. **崩溃恢复窗口**：系统重启后仍能清理

## 🔧 实现细节

### 数据库字段
```java
@Entity
public class SysFile {
    private String storagePath;      // 当前文件路径
    private String oldStoragePath;   // 旧文件路径（待删除）
    private LocalDateTime deleteAfter; // 删除时间
    @Version
    private Long version;            // 乐观锁版本号
}
```

### 写操作流程
```java
@Transactional
public void write(VfsContext ctx, String path, byte[] content) {
    // 1. 上传新文件（不加锁，不影响读操作）
    String newStoragePath = uploadContent(content, fileName);

    // 2. 加悲观锁查询
    Optional<SysFile> file = findByPathForUpdate(ctx, path);

    // 3. 原子性更新指针
    String oldPath = file.getStoragePath();
    file.setStoragePath(newStoragePath);
    file.setOldStoragePath(oldPath);
    file.setDeleteAfter(now + 5min);

    // 4. 保存（事务提交时释放锁）
    fileRepository.save(file);
}
```

### 读操作流程
```java
public String read(VfsContext ctx, String path) {
    // 1. 查询文件记录（无锁，快照读）
    SysFile file = findByPath(ctx, path);

    // 2. 下载当前版本
    return storageService.download(file.getStoragePath());
}
```

## 📈 性能对比

| 操作 | 传统方式 | Copy-on-Write |
|------|---------|---------------|
| 读操作延迟 | 可能被写操作阻塞 | 无阻塞 |
| 写操作延迟 | 加锁 → 删除 → 上传 → 更新 | 上传 → 短暂加锁更新 |
| 读并发度 | 受写操作影响 | 完全并发 |
| 写并发度 | 串行化 | 串行化 |
| 存储开销 | 低 | 稍高（5分钟窗口期） |

## ✅ 优势总结

1. **读写分离**：写操作不阻塞读操作
2. **原子性**：指针更新是原子操作
3. **一致性**：读操作总是看到完整版本
4. **高并发**：读操作完全无锁
5. **崩溃安全**：任何时刻崩溃都能恢复

## ⚠️ 注意事项

1. **存储空间**：5 分钟窗口期内会有两份文件
2. **清理任务**：必须启用定时任务清理旧文件
3. **时钟同步**：分布式环境需要时钟同步
4. **大文件**：上传大文件时间较长，但不影响读操作

## 🚀 使用示例

```java
// 读操作（无锁，高性能）
String content = vfs.read(ctx, "/test.txt");
String withLineNumbers = vfs.catWithLineNumbers(ctx, "/test.txt");
String lastLines = vfs.tail(ctx, "/test.txt", 10);

// 写操作（Copy-on-Write）
vfs.write(ctx, "/test.txt", "new content");
// 写操作期间，其他线程仍能读取旧版本

// 并发场景
ExecutorService executor = Executors.newFixedThreadPool(10);

// 10 个线程同时读取（完全并发）
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        String content = vfs.read(ctx, "/test.txt");
        System.out.println(content);
    });
}

// 1 个线程写入（不阻塞读操作）
executor.submit(() -> {
    vfs.write(ctx, "/test.txt", "updated content");
});
```
