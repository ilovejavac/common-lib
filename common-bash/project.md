# common-bash

## 项目概述

Bash 命令执行框架，支持命令注册、别名、管道等功能。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-bash</artifactId>
</dependency>
```

### 2. 定义命令

```java
@Component
@Command(name = "ls", aliases = {"ll", "dir"}, description = "列出目录内容")
public class LsCommand extends BashCommand {
    
    @Autowired
    private VirtualFileSystem vfs;
    
    @Override
    public String execute(String[] args) {
        // args: [0]=命令名, [1..n]=参数
        String path = args.length > 1 ? args[1] : "/";
        
        VfsContext ctx = getContext();
        List<VfsNode> nodes = vfs.listDirectory(ctx, path, 1);
        
        StringBuilder sb = new StringBuilder();
        for (VfsNode node : nodes) {
            sb.append(node.getIsDirectory() ? "d" : "-")
              .append(node.getName())
              .append("\n");
        }
        return sb.toString();
    }
}
```

### 3. 使用命令处理器

```java
@Service
public class CommandService {
    
    @Autowired
    private CommandHandler commandHandler;
    
    public String execute(String input) {
        // input: "ls -l /home"
        return commandHandler.execute(input);
    }
}
```

## 面向 LLM

### 核心组件

1. **@Command**：命令注册注解
   - name: 命令名称
   - aliases: 命令别名
   - description: 命令描述

2. **BashCommand**：命令基类
   - execute(): 执行方法
   - getContext(): 获取上下文

3. **CommandHandler**：命令处理器
   - execute(input): 执行命令字符串

### 最近修改 (2026-01-09)

1. **命令格式重构**：
   - `CommandHandler` 接口签名从 `Object... args` 改为 `String[] args`
   - `BashCommand` 添加 `parseCommandLine(String)` 方法支持 shell-like 解析
   - 支持单引号、双引号、转义字符

### TODO

- [ ] 添加单元测试验证 `parseCommandLine()` 方法
- [ ] 测试各种命令格式：`ls`, `ls -la`, `cat 'file with spaces.txt'`
- [ ] 文档更新：更新示例代码使用新的接口签名

### 修改记录位置

- 核心代码：`common-bash/src/main/java/com/dev/lib/bash/`
- Git 提交：使用 `git log --oneline common-bash/` 查看
