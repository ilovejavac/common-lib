package com.dev.lib.storage.domain.command;

import com.dev.lib.bash.BashCommand;
import com.dev.lib.bash.CommandHandler;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VFS Bash 命令实现
 * 负责解析 bash 命令字符串并调用底层 VFS 接口
 */
@Component
@RequiredArgsConstructor
public class VfsBashCommand extends BashCommand {

    private final VirtualFileSystem vfs;

    private final Map<String, CommandHandler<VfsContext>> commands = new HashMap<>();

    {
        // 注册所有命令
        commands.put("ls", this::ls);
        commands.put("cat", this::cat);
        commands.put("head", this::head);
        commands.put("tail", this::tail);
        commands.put("echo", this::echo);
        commands.put("touch", this::touch);
        commands.put("cp", this::cp);
        commands.put("mv", this::mv);
        commands.put("rm", this::rm);
        commands.put("mkdir", this::mkdir);
        commands.put("find", this::find);
        commands.put("grep", this::grep);
    }

    /**
     * 执行命令
     * @param ctx VFS 上下文
     * @param commandLine 完整命令行，如 "ls -la /path"
     * @return 执行结果
     */
    public Object execute(VfsContext ctx, String commandLine) {

        if (commandLine == null || commandLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Command line cannot be empty");
        }

        String[] tokens = parseCommandLine(commandLine);
        if (tokens.length == 0) {
            throw new IllegalArgumentException("Command line cannot be empty");
        }

        String commandName = tokens[0];
        String[] commandArgs = tokens.length > 1
                ? Arrays.copyOfRange(tokens, 1, tokens.length)
                : new String[0];

        CommandHandler handler = commands.get(commandName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }

        return handler.handle(ctx, commandArgs);
    }

    /**
     * ls [-d depth] [path]
     */
    private Object ls(VfsContext ctx, String[] args) {

        ParsedArgs parsed = parseArgs(args);
        String     path   = parsed.getString(0);
        if (path == null) path = "/";

        Integer depth = parsed.getInt("d", 1);
        return vfs.listDirectory(ctx, path, depth);
    }

    /**
     * cat [-n] <file> [file...]
     * cat > <file> << <content>
     * cat >> <file> << <content>
     *
     * -n: 显示行号
     * > <<: 覆盖写入（heredoc风格）
     * >> <<: 追加写入（heredoc风格）
     *
     * 读取文件或写入内容到文件
     */
    private Object cat(VfsContext ctx, String[] args) {

        // 检查是否是写入模式（heredoc: cat > file << content）
        boolean isWriteMode = false;
        boolean isAppendMode = false;
        String filePath = null;
        int heredocIndex = -1;

        for (int i = 0; i < args.length; i++) {
            if (">>".equals(args[i])) {
                isAppendMode = true;
                isWriteMode = true;
                if (i + 2 < args.length && "<<".equals(args[i + 2])) {
                    filePath = args[i + 1];
                    heredocIndex = i + 3;
                    break;
                }
            } else if (">".equals(args[i])) {
                isWriteMode = true;
                if (i + 2 < args.length && "<<".equals(args[i + 2])) {
                    filePath = args[i + 1];
                    heredocIndex = i + 3;
                    break;
                }
            }
        }

        // 写入模式
        if (isWriteMode && filePath != null && heredocIndex >= 0) {
            StringBuilder content = new StringBuilder();
            for (int i = heredocIndex; i < args.length; i++) {
                if (content.length() > 0) {
                    content.append(" ");
                }
                content.append(args[i]);
            }

            String finalContent = content.toString();
            if (isAppendMode) {
                // 追加
                vfs.appendFile(ctx, filePath, finalContent);
            } else {
                // 覆盖
                vfs.writeFile(ctx, filePath, finalContent);
            }
            return null;
        }

        // 读取模式（原有逻辑）
        ParsedArgs parsed          = parseArgs(args);
        boolean    showLineNumbers = parsed.hasFlag("n");
        Integer    startLine       = parsed.getInt("s", 1);
        Integer    lineCount       = parsed.getInt("c", -1);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("cat: missing file operand");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);

            if (startLine > 1 || lineCount != -1) {
                // 使用行范围读取（避免 OOM）
                List<String> lines   = vfs.readLines(ctx, path, startLine, lineCount);
                int          lineNum = startLine;
                for (String line : lines) {
                    if (showLineNumbers) {
                        result.append(String.format("%6d\t%s\n", lineNum++, line));
                    } else {
                        result.append(line).append("\n");
                    }
                }
            } else {
                // 流式读取（避免 OOM）
                try (InputStream is = vfs.openFile(ctx, path);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String line;
                    int    lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        if (showLineNumbers) {
                            result.append(String.format("%6d\t%s\n", lineNum++, line));
                        } else {
                            result.append(line).append("\n");
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read file: " + path, e);
                }
            }
        }

        return result.toString();
    }

    /**
     * head [-n lines] <file>
     */
    private Object head(VfsContext ctx, String[] args) {

        ParsedArgs parsed = parseArgs(args);
        int        lines  = parsed.getInt("n", 100);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("head: missing file operand");
        }

        String       path      = parsed.getString(0);
        List<String> fileLines = vfs.readLines(ctx, path, 1, lines);
        return String.join("\n", fileLines) + "\n";
    }

    /**
     * tail [-n lines] <file>
     * 使用循环缓冲区，只读取文件一次
     */
    private Object tail(VfsContext ctx, String[] args) {

        ParsedArgs parsed = parseArgs(args);
        int        lines  = parsed.getInt("n", 100);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("tail: missing file operand");
        }

        String path = parsed.getString(0);

        // 使用循环缓冲区，只读取文件一次
        try (InputStream is = vfs.openFile(ctx, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String[] buffer = new String[lines];
            int      index  = 0;
            int      count  = 0;
            String   line;

            while ((line = reader.readLine()) != null) {
                buffer[index] = line;
                index = (index + 1) % lines;
                count++;
            }

            // 构建结果：从最旧的行开始
            StringBuilder result = new StringBuilder();
            int           start  = count < lines ? 0 : index;
            int           end    = Math.min(count, lines);

            for (int i = 0; i < end; i++) {
                result.append(buffer[(start + i) % lines]).append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    /**
     * echo <content> > <file>
     * echo <content> >> <file>
     *
     * 输出内容到文件（支持重定向）
     * 示例：
     * echo "hello world" > /file.txt
     * echo "new line" >> /file.txt
     * echo "line1\nline2\nline3" > /file.txt
     */
    private Object echo(VfsContext ctx, String[] args) {

        if (args.length == 0) {
            throw new IllegalArgumentException("echo: missing content");
        }

        // 解析重定向符 >
        boolean append = false;
        String  filePath = null;
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (">>".equals(arg)) {
                append = true;
                if (i + 1 < args.length) {
                    filePath = args[++i];
                }
            } else if (">".equals(arg)) {
                append = false;
                if (i + 1 < args.length) {
                    filePath = args[++i];
                }
            } else {
                if (content.length() > 0) {
                    content.append(" ");
                }
                content.append(arg);
            }
        }

        // 如果有重定向，写入文件
        if (filePath != null) {
            String finalContent = content.toString();
            if (append) {
                // 追加模式
                vfs.appendFile(ctx, filePath, finalContent);
            } else {
                // 覆盖模式
                vfs.writeFile(ctx, filePath, finalContent);
            }
            return null;
        }

        // 没有重定向，返回内容
        return content.toString();
    }

    /**
     * touch <file> [file...]
     */
    private Object touch(VfsContext ctx, String[] args) {

        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("touch: missing file operand");
        }

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            vfs.touchFile(ctx, path);
        }

        return null;
    }

    /**
     * cp <src> <dest>
     * cp <src...> <dest_dir>
     *
     * 复制文件或目录，自动检测目录并递归复制
     */
    private Object cp(VfsContext ctx, String[] args) {

        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("cp: missing destination file operand");
        }

        if (parsed.positionalCount() == 2) {
            // 单个源
            String src  = parsed.getString(0);
            String dest = parsed.getString(1);
            // 自动检测是否为目录
            boolean recursive = vfs.isDirectory(ctx, src);
            vfs.copy(ctx, src, dest, recursive);
        } else {
            // 多个源文件，目标必须是目录
            String destDir = parsed.getString(parsed.positionalCount() - 1);
            for (int i = 0; i < parsed.positionalCount() - 1; i++) {
                String src = parsed.getString(i);
                // 自动检测是否为目录
                boolean recursive = vfs.isDirectory(ctx, src);
                vfs.copy(ctx, src, destDir, recursive);
            }
        }

        return null;
    }

    /**
     * mv <src> <dest>
     * mv <src...> <dest_dir>
     */
    private Object mv(VfsContext ctx, String[] args) {

        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("mv: missing destination file operand");
        }

        if (parsed.positionalCount() == 2) {
            // 单个源文件
            String src  = parsed.getString(0);
            String dest = parsed.getString(1);
            vfs.move(ctx, src, dest);
        } else {
            // 多个源文件，目标必须是目录
            String destDir = parsed.getString(parsed.positionalCount() - 1);
            for (int i = 0; i < parsed.positionalCount() - 1; i++) {
                String src = parsed.getString(i);
                vfs.move(ctx, src, destDir);
            }
        }

        return null;
    }

    /**
     * rm [-rf] <file> [file...]
     */
    private Object rm(VfsContext ctx, String[] args) {

        ParsedArgs parsed    = parseArgs(args);
        boolean    recursive = parsed.hasFlag("r") || parsed.hasFlag("f");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("rm: missing operand");
        }

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            vfs.delete(ctx, path, recursive);
        }

        return null;
    }

    /**
     * mkdir [-p] <dir> [dir...]
     */
    private Object mkdir(VfsContext ctx, String[] args) {

        ParsedArgs parsed        = parseArgs(args);
        boolean    createParents = parsed.hasFlag("p");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("mkdir: missing operand");
        }

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            vfs.createDirectory(ctx, path, createParents);
        }

        return null;
    }

    /**
     * find [-r] <basePath> -name <pattern>
     */
    private Object find(VfsContext ctx, String[] args) {

        ParsedArgs parsed    = parseArgs(args);
        boolean    recursive = parsed.hasFlag("r");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("find: missing operand");
        }

        String basePath = parsed.getString(0);
        String pattern  = parsed.getString(1);

        return vfs.findByName(ctx, basePath, pattern, recursive);
    }

    /**
     * grep [-r] <content> <basePath>
     */
    private Object grep(VfsContext ctx, String[] args) {

        ParsedArgs parsed    = parseArgs(args);
        boolean    recursive = parsed.hasFlag("r");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("grep: missing operand");
        }

        String content  = parsed.getString(0);
        String basePath = parsed.getString(1);

        return vfs.findByContent(ctx, basePath, content, recursive);
    }

}
