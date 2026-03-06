# Bash 命令使用指南

## 架构设计

```
common-core (基础工具)
    ↓
common-storage (文件存储 + VFS 文件系统)
    ↓
common-bash (bash 命令实现 + 统一入口)
```

## 核心特性

### ✅ 完整的 Bash 语法支持

- **管道操作**：`cat file.txt | grep pattern | wc -l`
- **命令链**：`cd /path && ls -la`
- **变量替换**：`export VAR=value && echo $VAR`
- **控制流**：`if [ -f file ]; then cat file; fi`
- **循环**：`for f in *.txt; do cat $f; done`
- **重定向**：`cat file.txt > output.txt`

### 自动检测机制

Bash 入口会自动检测命令复杂度：
- **简单命令**：直接使用 VFS 命令（高性能）
- **复杂命令**：自动切换到 vfsbash（完整 bash 支持）

## 使用方式

### 1. 简单命令（推荐）

```java
// 直接使用 VFS 命令，高性能
Bash.exec("ls -la /bucket");
Bash.exec("cat /bucket/file.txt");
Bash.exec("grep -r 'pattern' /logs");
Bash.exec("find /bucket -name '*.log'");
```

### 2. 管道操作

```java
// 自动检测管道，使用 vfsbash
String result = (String) Bash.exec("cat /bucket/file.txt | grep error | wc -l");

// 复杂管道
String result = (String) Bash.exec(
    "find /logs -name '*.log' | xargs grep 'ERROR' | sort | uniq -c"
);
```

### 3. 命令链（cd && xxx）

```java
// 自动检测 &&，使用 vfsbash
String result = (String) Bash.exec("cd /bucket/data && ls -la");

// 多个命令链
String result = (String) Bash.exec(
    "cd /bucket && mkdir -p temp && cp *.txt temp/"
);
```

### 4. 变量替换

```java
// 自动检测变量 $，使用 vfsbash
String result = (String) Bash.exec(
    "export DIR=/bucket/logs && cd $DIR && ls"
);

// 命令替换
String result = (String) Bash.exec(
    "echo \"Total files: $(find /bucket -type f | wc -l)\""
);
```

### 5. 控制流（if/for/while）

```java
// if 条件
String result = (String) Bash.exec(
    "if [ -f /bucket/config.txt ]; then cat /bucket/config.txt; else echo 'not found'; fi"
);

// for 循环
String result = (String) Bash.exec(
    "for f in /bucket/*.txt; do echo \"Processing $f\"; cat $f | wc -l; done"
);

// while 循环
String result = (String) Bash.exec(
    "while read line; do echo \"Line: $line\"; done < /bucket/data.txt"
);
```

### 6. 链式调用

```java
// 指定根路径
Bash.root("/bucket").exec("ls -la");
Bash.root("/bucket").exec("cat file.txt | grep error");
```

## 命令对比

### VFS 命令 vs VfsBash

| 特性 | VFS 命令 | VfsBash |
|------|---------|---------|
| 性能 | ⚡ 高（直接操作 VFS） | 🐢 中（需要路径映射） |
| 管道 | ❌ 不支持 | ✅ 支持 |
| 变量 | ❌ 不支持 | ✅ 支持 |
| 控制流 | ❌ 不支持 | ✅ 支持 |
| 命令链 | ❌ 不支持 | ✅ 支持 |
| 使用场景 | 简单文件操作 | 复杂脚本逻辑 |

### 自动选择策略

```java
// 简单命令 → 使用 VFS 命令
Bash.exec("ls -la /bucket");           // ✅ VFS 命令
Bash.exec("cat /bucket/file.txt");     // ✅ VFS 命令
Bash.exec("grep -r 'error' /logs");    // ✅ VFS 命令

// 复杂命令 → 自动切换到 VfsBash
Bash.exec("cat file.txt | grep error");              // ✅ VfsBash
Bash.exec("cd /bucket && ls");                       // ✅ VfsBash
Bash.exec("export VAR=value && echo $VAR");          // ✅ VfsBash
Bash.exec("if [ -f file ]; then cat file; fi");      // ✅ VfsBash
Bash.exec("for f in *.txt; do cat $f; done");        // ✅ VfsBash
```

## 实际场景示例

### 场景 1：日志分析

```java
// 统计错误日志数量
String count = (String) Bash.exec(
    "grep -r 'ERROR' /logs | wc -l"
);

// 查找最近的错误
String errors = (String) Bash.exec(
    "find /logs -name '*.log' -mtime -1 | xargs grep 'ERROR' | tail -n 50"
);

// 按错误类型分组统计
String stats = (String) Bash.exec(
    "grep -r 'ERROR' /logs | awk '{print $3}' | sort | uniq -c | sort -rn"
);
```

### 场景 2：数据处理

```java
// CSV 数据提取和转换
String result = (String) Bash.exec(
    "cat /data/users.csv | awk -F',' '{print $1,$3}' | sort | uniq > /data/output.txt"
);

// 批量文件处理
String result = (String) Bash.exec(
    "for f in /data/*.json; do " +
    "  echo \"Processing $f\"; " +
    "  cat $f | jq '.name' >> /data/names.txt; " +
    "done"
);
```

### 场景 3：文件管理

```java
// 条件清理
String result = (String) Bash.exec(
    "if [ $(find /tmp -name '*.tmp' | wc -l) -gt 100 ]; then " +
    "  find /tmp -name '*.tmp' -mtime +7 -delete; " +
    "fi"
);

// 批量重命名
String result = (String) Bash.exec(
    "cd /bucket && for f in *.txt; do " +
    "  mv \"$f\" \"backup_$(date +%Y%m%d)_$f\"; " +
    "done"
);
```

### 场景 4：复杂脚本

```java
// 多步骤数据处理流程
String result = (String) Bash.exec(
    "cd /data && " +
    "mkdir -p processed && " +
    "for f in raw/*.csv; do " +
    "  filename=$(basename $f); " +
    "  cat $f | grep -v '^#' | awk -F',' '{print $1,$2}' > processed/$filename; " +
    "  echo \"Processed $filename\"; " +
    "done && " +
    "cat processed/*.csv > all_data.csv && " +
    "echo \"Total lines: $(wc -l < all_data.csv)\""
);
```

## VfsBash 工作原理

### 路径映射机制

1. **检测 VFS 路径**：扫描命令中的路径（如 `/bucket/file.txt`）
2. **下载到临时目录**：将 VFS 文件下载到本地临时目录
3. **路径替换**：将命令中的 VFS 路径替换为临时路径
4. **执行 Bash**：在临时目录中执行真实的 bash 命令
5. **清理**：自动删除临时文件

### 示例

```java
// 原始命令
Bash.exec("cat /bucket/file.txt | grep error");

// 内部处理流程
// 1. 检测到管道 |，自动使用 vfsbash
// 2. 下载 /bucket/file.txt → /tmp/vfsbash-xxx/bucket/file.txt
// 3. 替换路径：cat /tmp/vfsbash-xxx/bucket/file.txt | grep error
// 4. 执行 bash -lc "cat /tmp/vfsbash-xxx/bucket/file.txt | grep error"
// 5. 返回结果，清理 /tmp/vfsbash-xxx
```

## 性能建议

1. **简单操作优先使用 VFS 命令**
   ```java
   // ✅ 推荐：直接使用 VFS 命令
   Bash.exec("cat /bucket/file.txt");

   // ❌ 不推荐：不必要的复杂度
   Bash.exec("cat /bucket/file.txt | cat");
   ```

2. **批量操作使用管道**
   ```java
   // ✅ 推荐：一次性处理
   Bash.exec("find /logs -name '*.log' | xargs grep 'ERROR'");

   // ❌ 不推荐：多次调用
   List<VfsNode> files = (List<VfsNode>) Bash.exec("find /logs -name '*.log'");
   for (VfsNode file : files) {
       Bash.exec("grep 'ERROR' " + file.getPath());
   }
   ```

3. **大文件操作直接使用 Vfs API**
   ```java
   // ✅ 推荐：流式处理
   try (InputStream is = Vfs.openFile(ctx, path)) {
       // 处理大文件
   }

   // ❌ 不推荐：全量加载
   String content = (String) Bash.exec("cat /bucket/large_file.txt");
   ```

## 注意事项

1. **VfsBash 会创建临时文件**：适合中小型文件，大文件建议直接使用 Vfs API
2. **路径必须是绝对路径**：VFS 路径必须以 `/` 开头
3. **自动清理**：临时文件会在命令执行后自动清理
4. **超时设置**：默认 300 秒，长时间运行的脚本需要注意

## 支持的命令列表

### VFS 命令（高性能）
- `ls` - 列出目录
- `cat` - 查看文件
- `head` - 查看文件头部
- `tail` - 查看文件尾部
- `find` - 查找文件
- `grep` - 搜索内容
- `touch` - 创建文件
- `mkdir` - 创建目录
- `rm` - 删除文件
- `cp` - 复制文件
- `mv` - 移动文件

### VfsBash（完整 Bash）
- 所有 Linux 命令
- 管道、重定向
- 变量、命令替换
- 控制流（if/for/while）
- 命令链（&&/||/;）
