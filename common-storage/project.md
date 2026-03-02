# common-storage

## 项目概述

统一文件存储与虚拟文件系统能力模块。

- 对外统一入口：`Storage`（对象存储静态工具）与 `Vfs`（虚拟文件系统静态工具）
- 支持后端：Local / MinIO / OSS / RustFS（通过 `ChainStorageService` 适配）
- 支持 Bash 风格命令执行：`POST /sys/bash/exec`

## 开发者使用

### 1. 依赖

```xml
<dependency>
  <groupId>com.dev.lib</groupId>
  <artifactId>common-storage</artifactId>
</dependency>
```

### 2. 配置

```yaml
app:
  storage:
    type: local # local|minio|oss|rustfs
    local:
      path: /data/files
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: vfs
    oss:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      access-key: your-access-key
      secret-key: your-secret-key
      bucket: my-bucket
    vfs:
      temporary-ttl-minutes: 60
      cleanup-interval-ms: 300000
      cleanup-initial-delay-ms: 60000
```

### 3. Storage 静态工具（对象存储）

```java
// 写入
Storage.bucket("my-bucket").object("a/b/test.txt").write("hello");

// 读取
String content = Storage.bucket("my-bucket").object("a/b/test.txt").read();

// 下载流
InputStream is = Storage.bucket("my-bucket").object("a/b/test.txt").download();

// 预签名
String url = Storage.bucket("my-bucket").object("a/b/test.txt").presignedUrl(3600);

// 删除
Storage.bucket("my-bucket").object("a/b/test.txt").delete();
```

### 4. Vfs 静态工具（虚拟文件系统）

```java
VfsContext ctx = VfsContext.of("/workspace");

// 写读
Vfs.writeFile(ctx, "/demo.txt", "hello");
String content = Vfs.readFile(ctx, "/demo.txt");

// 目录
Vfs.createDirectory(ctx, "/a/b/c", true);
List<VfsNode> nodes = Vfs.listDirectory(ctx, "/a", 2);

// 文件系统操作
Vfs.copy(ctx, "/demo.txt", "/demo2.txt", false);
Vfs.move(ctx, "/demo2.txt", "/archive/demo2.txt");
Vfs.delete(ctx, "/archive", true);

// 搜索
List<VfsNode> byName = Vfs.findByName(ctx, "/", "*.txt", true);
List<VfsNode> byContent = Vfs.findByContent(ctx, "/", "hello", true);
```

### 5. 临时文件与清理

```java
VfsContext ctx = VfsContext.of("/workspace");
ctx.setTemporary(true); // 标记临时文件
// ctx.setExpirationAt(LocalDateTime.now().plusMinutes(30)); // 可选，未设置走默认 TTL

Vfs.uploadFile(ctx, "/tmp/a.txt", inputStream, size);
```

- 清理任务只清理当前 `spring.application.name` 对应服务的文件
- 通过 `SysFile.serviceName` 做归属隔离

### 6. Bash 命令接口

```http
POST /sys/bash/exec
Content-Type: application/json

{
  "root": "/workspace",
  "command": "ls -R /"
}
```

支持命令：`ls cat head tail echo write touch cp mv rm mkdir find grep sed`

## 当前架构（2026-03）

- `Storage`：对象存储静态 API
- `Vfs`：虚拟文件系统静态 API
- `VirtualFileSystemImpl`：VFS 核心实现（内部）
- `ChainStorageService`：多后端适配接口（内部）
- `VfsCleanupTask + VfsAsyncCleanupService`：旧版本与过期临时文件异步清理

## 已移除

- `FileService` / `FileServiceImpl`
- `VirtualFileSystem` 接口
- `StorageService` 接口及其旧实现

## 关键源码位置

- `common-storage/src/main/java/com/dev/lib/storage/Storage.java`
- `common-storage/src/main/java/com/dev/lib/storage/Vfs.java`
- `common-storage/src/main/java/com/dev/lib/storage/domain/service/virtual/VirtualFileSystemImpl.java`
- `common-storage/src/main/java/com/dev/lib/storage/domain/service/chain/`
- `common-storage/src/main/java/com/dev/lib/storage/trigger/schedule/VfsCleanupTask.java`
