# common-storage

## 项目概述

虚拟文件系统 (VFS) 和统一存储服务，提供类 Linux 文件操作的抽象层，支持多种存储后端（本地、MinIO、阿里云 OSS、RustFS）。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-storage</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
app:
  storage:
    type: local  # local, minio, oss, rustfs
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
```

### 3. 使用方式

#### 3.1 注入 VFS 服务

```java
@Service
public class FileService {
    
    @Autowired
    private VirtualFileSystem vfs;
    
    // 创建上下文
    private VfsContext getContext() {
        VfsContext ctx = new VfsContext();
        ctx.setRoot("/home/user");  // 设置根目录
        ctx.setUserId("user123");
        return ctx;
    }
}
```

#### 3.2 文件操作

```java
// 写入文件
vfs.writeFile(ctx, "/test.txt", "Hello World");
vfs.writeFile(ctx, "/data.json", "{\"key\": \"value\"}".getBytes(UTF_8));

// 读取文件
String content = vfs.readFile(ctx, "/test.txt");

// 读取指定行（大文件）
List<String> lines = vfs.readLines(ctx, "/large.log", 1, 100);

// 获取文件行数
int count = vfs.getLineCount(ctx, "/large.log");

// 创建空文件
vfs.touchFile(ctx, "/empty.txt");
```

#### 3.3 目录操作

```java
// 创建目录（递归）
vfs.createDirectory(ctx, "/a/b/c", true);

// 列出目录（depth: 1=仅子项, 2=递归2层, 3=递归3层）
List<VfsNode> nodes = vfs.listDirectory(ctx, "/home", 1);
for (VfsNode node : nodes) {
    System.out.println(node.getName() + " " + node.getIsDirectory());
}
```

#### 3.4 复制/移动/删除

```java
// 复制文件
vfs.copy(ctx, "/src.txt", "/dest.txt", false);

// 递归复制目录
vfs.copy(ctx, "/src-folder", "/dest-folder", true);

// 移动/重命名
vfs.move(ctx, "/old.txt", "/new.txt");
vfs.move(ctx, "/old-folder", "/new-folder");

// 删除文件
vfs.delete(ctx, "/file.txt", false);

// 递归删除目录
vfs.delete(ctx, "/folder", true);
```

#### 3.5 搜索功能

```java
// 按文件名搜索（支持通配符）
List<VfsNode> results = vfs.findByName(ctx, "/home", "*.txt", true);

// 搜索文件内容
List<VfsNode> grepResults = vfs.findByContent(ctx, "/home", "keyword", true);
```

#### 3.6 文件上传

```java
// 上传 ZIP 并解压
List<String> fileIds = vfs.uploadZip(ctx, "/target", zipInputStream);

// 批量上传文件
MultipartFile[] files = ...;
String[] relativePaths = {"folder/file1.txt", "folder/file2.jpg"};
List<String> ids = vfs.uploadFiles(ctx, "/target", files, relativePaths);
```

#### 3.7 检查操作

```java
// 检查路径是否存在
boolean exists = vfs.exists(ctx, "/file.txt");

// 检查是否为目录
boolean isDir = vfs.isDirectory(ctx, "/folder");
```

## 面向 LLM

### 核心架构

1. **领域模型**
   - `VfsContext`：执行上下文（包含根目录、用户信息）
   - `VfsNode`：文件节点（目录/文件）
   - `SysFile`：数据库实体（存储虚拟路径与物理路径映射）

2. **核心服务**
   - `VirtualFileSystem`：VFS 接口
   - `VirtualFileSystemImpl`：核心实现，严格遵循 Linux 文件系统语义
   - `StorageService`：统一存储接口
   - `FileService`：文件管理服务

3. **存储适配器**
   - `LocalFileStorage`：本地文件系统
   - `MinioFileStorage`：MinIO 对象存储
   - `OssFileStorage`：阿里云 OSS
   - `RustfsStorage`：RustFS 高性能存储

### 最近修改 (2025-01-08)

1. **normalizePath** - 修复根路径 `/` 返回空字符串的问题
2. **move** - 环检测移到目标重计算之后，防止检测失效
3. **uploadFiles** - 文件已存在时报错（同步 Linux 行为）
4. **uploadZip** - 文件已存在时报错（同步 Linux 行为）

### 关键设计决策

1. **路径规范化**：空栈返回 `/` 而不是空字符串
2. **目录自动创建**：move、writeFile 时自动创建父目录（与标准 Linux 不同）
3. **Copy-on-Write**：文件更新时先写新文件，再原子替换指针
4. **悲观锁**：写操作使用 `FOR UPDATE` 防止并发冲突

### 修改记录位置

- 核心实现：`common-storage/src/main/java/com/dev/lib/storage/domain/service/VirtualFileSystemImpl.java`
- Git 提交：使用 `git log --oneline common-storage/` 查看
