package com.dev.lib.storage.domain.command;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VFS Bash 命令实现
 * 负责解析 bash 命令参数并调用底层 VFS 接口
 */
@Component
@RequiredArgsConstructor
public class VfsBashCommand extends BashCommand {

    private final VirtualFileSystem vfs;

    private final Map<String, CommandHandler> commands = new HashMap<>();

    {
        // 注册所有命令
        commands.put("ls", this::ls);
        commands.put("cat", this::cat);
        commands.put("head", this::head);
        commands.put("tail", this::tail);
        commands.put("touch", this::touch);
        commands.put("cp", this::cp);
        commands.put("mv", this::mv);
        commands.put("rm", this::rm);
        commands.put("mkdir", this::mkdir);
        commands.put("find", this::find);
        commands.put("grep", this::grep);
    }

    @Override
    public Object execute(Object... args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: execute(VfsContext, commandName, ...args)");
        }

        VfsContext ctx = (VfsContext) args[0];
        String commandName = (String) args[1];

        // 提取命令参数（跳过 ctx 和 commandName）
        Object[] commandArgs = new Object[args.length - 2];
        System.arraycopy(args, 2, commandArgs, 0, commandArgs.length);

        CommandHandler handler = commands.get(commandName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }

        return handler.handle(ctx, commandArgs);
    }

    /**
     * ls [-l] [path]
     */
    private Object ls(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        String path = parsed.getString(0);
        if (path == null) path = "/";

        Integer depth = parsed.getInt("d", 1);
        return vfs.listDirectory(ctx, path, depth);
    }

    /**
     * cat [-n] <file> [file...]
     * -n: 显示行号
     */
    private Object cat(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        boolean showLineNumbers = parsed.hasFlag("n");
        Integer startLine = parsed.getInt("s", 1);
        Integer lineCount = parsed.getInt("c", -1);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("cat: missing file operand");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);

            if (startLine > 1 || lineCount != -1) {
                // 使用行范围读取（避免 OOM）
                List<String> lines = vfs.readLines(ctx, path, startLine, lineCount);
                int lineNum = startLine;
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
                    int lineNum = 1;
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
    private Object head(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        int lines = parsed.getInt("n", 10);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("head: missing file operand");
        }

        String path = parsed.getString(0);
        List<String> fileLines = vfs.readLines(ctx, path, 1, lines);
        return String.join("\n", fileLines) + "\n";
    }

    /**
     * tail [-n lines] <file>
     */
    private Object tail(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        int lines = parsed.getInt("n", 10);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("tail: missing file operand");
        }

        String path = parsed.getString(0);
        int totalLines = vfs.getLineCount(ctx, path);
        int startLine = Math.max(1, totalLines - lines + 1);

        List<String> fileLines = vfs.readLines(ctx, path, startLine, -1);
        return String.join("\n", fileLines) + "\n";
    }

    /**
     * touch <file> [file...]
     */
    private Object touch(VfsContext ctx, Object... args) {
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
     * cp [-r] <src> <dest>
     * cp [-r] <src...> <dest_dir>
     */
    private Object cp(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("cp: missing destination file operand");
        }

        if (parsed.positionalCount() == 2) {
            // 单个源文件
            String src = parsed.getString(0);
            String dest = parsed.getString(1);
            vfs.copy(ctx, src, dest, recursive);
        } else {
            // 多个源文件，目标必须是目录
            String destDir = parsed.getString(parsed.positionalCount() - 1);
            for (int i = 0; i < parsed.positionalCount() - 1; i++) {
                String src = parsed.getString(i);
                vfs.copy(ctx, src, destDir, recursive);
            }
        }

        return null;
    }

    /**
     * mv <src> <dest>
     * mv <src...> <dest_dir>
     */
    private Object mv(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("mv: missing destination file operand");
        }

        if (parsed.positionalCount() == 2) {
            // 单个源文件
            String src = parsed.getString(0);
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
    private Object rm(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r") || parsed.hasFlag("f");

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
    private Object mkdir(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        boolean createParents = parsed.hasFlag("p");

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
    private Object find(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("find: missing operand");
        }

        String basePath = parsed.getString(0);
        String pattern = parsed.getString(1);

        return vfs.findByName(ctx, basePath, pattern, recursive);
    }

    /**
     * grep [-r] <content> <basePath>
     */
    private Object grep(VfsContext ctx, Object... args) {
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("grep: missing operand");
        }

        String content = parsed.getString(0);
        String basePath = parsed.getString(1);

        return vfs.findByContent(ctx, basePath, content, recursive);
    }

    @FunctionalInterface
    private interface CommandHandler {
        Object handle(VfsContext ctx, Object... args);
    }
}
