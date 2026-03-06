package com.dev.lib.bash.vfs;

import com.dev.lib.bash.BashCommand;
import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VFS Bash 命令 - 支持完整的 bash 语法（管道、变量、控制流等）
 *
 * 工作原理：
 * 1. 将 VFS 路径映射到临时本地文件系统
 * 2. 执行真实的 bash 命令
 * 3. 将结果回写到 VFS（如果需要）
 * 4. 清理临时文件
 *
 * 支持的特性：
 * - 管道：cat file.txt | grep pattern | wc -l
 * - 变量：export VAR=value && echo $VAR
 * - 控制流：if [ -f file ]; then cat file; fi
 * - 循环：for f in *.txt; do cat $f; done
 * - 命令链：cd /path && ls -la
 *
 * 用法：
 * - Bash.exec("vfsbash -c 'cd /bucket && ls | grep .log'")
 * - Bash.root("/bucket").exec("vfsbash 'cat *.txt | grep error'")
 */
public class VfsBashCommand extends BashCommand<String> {

    private static final long DEFAULT_TIMEOUT_SECONDS = 300;
    private static final Pattern VFS_PATH_PATTERN = Pattern.compile("(/[^\\s;|&<>()]+)");

    @Override
    public String execute(ExecuteContext ctx) {
        String[] tokens = parseCommandLine(ctx.getCommand());
        if (tokens.length < 2) {
            throw new IllegalArgumentException("vfsbash: missing script");
        }

        String[] args = java.util.Arrays.copyOfRange(tokens, 1, tokens.length);
        String script = resolveScript(args);

        VfsContext vfsCtx = VfsContext.of(ctx.getRoot());

        // 创建临时工作目录
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("vfsbash-");

            // 映射 VFS 路径到临时目录
            String mappedScript = mapVfsPathsToLocal(script, vfsCtx, tempDir);

            // 执行 bash 命令
            String output = executeBash(mappedScript, tempDir);

            // 回写修改的文件到 VFS（可选）
            // syncBackToVfs(tempDir, vfsCtx);

            return output;

        } catch (IOException e) {
            throw new RuntimeException("vfsbash: failed to create temp directory", e);
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    private String resolveScript(String[] args) {
        if (args.length >= 2 && "-c".equals(args[0])) {
            return args[1];
        }
        return String.join(" ", args);
    }

    /**
     * 将 VFS 路径映射到本地临时目录
     */
    private String mapVfsPathsToLocal(String script, VfsContext vfsCtx, Path tempDir) throws IOException {
        Matcher matcher = VFS_PATH_PATTERN.matcher(script);
        List<String> vfsPaths = new ArrayList<>();

        while (matcher.find()) {
            String path = matcher.group(1);
            if (Vfs.exists(vfsCtx, path)) {
                vfsPaths.add(path);
            }
        }

        // 下载 VFS 文件到临时目录
        for (String vfsPath : vfsPaths) {
            downloadVfsToLocal(vfsCtx, vfsPath, tempDir);
        }

        // 替换脚本中的路径
        String mappedScript = script;
        for (String vfsPath : vfsPaths) {
            String localPath = tempDir.resolve(vfsPath.substring(1)).toString();
            mappedScript = mappedScript.replace(vfsPath, localPath);
        }

        return mappedScript;
    }

    /**
     * 下载 VFS 文件/目录到本地
     */
    private void downloadVfsToLocal(VfsContext vfsCtx, String vfsPath, Path tempDir) throws IOException {
        if (Vfs.isDirectory(vfsCtx, vfsPath)) {
            // 递归下载目录
            List<VfsNode> nodes = Vfs.listDirectory(vfsCtx, vfsPath, 10);
            for (VfsNode node : nodes) {
                if (!Boolean.TRUE.equals(node.getIsDirectory())) {
                    downloadFileToLocal(vfsCtx, node.getPath(), tempDir);
                }
            }
        } else {
            // 下载单个文件
            downloadFileToLocal(vfsCtx, vfsPath, tempDir);
        }
    }

    private void downloadFileToLocal(VfsContext vfsCtx, String vfsPath, Path tempDir) throws IOException {
        Path localPath = tempDir.resolve(vfsPath.substring(1));
        Files.createDirectories(localPath.getParent());

        try (InputStream is = Vfs.openFile(vfsCtx, vfsPath)) {
            Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 执行 bash 命令
     */
    private String executeBash(String script, Path workDir) {
        ProcessBuilder pb = new ProcessBuilder("bash", "-lc", script);
        pb.redirectErrorStream(true);
        pb.directory(workDir.toFile());

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("vfsbash: command timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("vfsbash: exited with code " + exitCode +
                    (output.isBlank() ? "" : "\n" + output));
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("vfsbash: execute failed", e);
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
