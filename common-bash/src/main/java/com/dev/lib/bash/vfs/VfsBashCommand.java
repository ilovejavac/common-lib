package com.dev.lib.bash.vfs;

import com.dev.lib.bash.BashCommand;
import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * VFS Bash 命令 - 透明集成，双向同步，流式处理
 * <p>
 * 工作原理：
 * 1. 解析脚本中引用的 VFS 路径
 * 2. 流式下载文件到临时目录，记录文件快照
 * 3. 执行原生 Bash 命令（无需特殊前缀）
 * 4. 检测修改的文件，流式上传回 VFS
 * 5. 检测新创建的文件，流式上传到 VFS
 * 6. 检测删除的文件，从 VFS 删除
 * 7. 清理临时目录
 * <p>
 * 用法：
 * <pre>
 * // 直接使用原生 Linux 命令
 * Bash.root("/bucket").exec("cat app.log | grep ERROR | wc -l");
 *
 * // 文件编辑，自动同步回 VFS
 * Bash.root("/logs").exec("sed -i 's/ERROR/WARN/g' app.log");
 *
 * // 批量处理
 * Bash.root("/data").exec("for f in *.txt; do cat $f | sort | uniq > ${f}.sorted; done");
 * </pre>
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

        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
        String script = resolveScript(args);

        VfsContext vfsCtx = VfsContext.of(ctx.getRoot());

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("vfsbash-");

            // 1. 解析脚本中引用的 VFS 路径
            Set<String> vfsPaths = extractVfsPaths(script, vfsCtx);

            // 2. 流式下载文件到临时目录，记录快照
            Map<String, FileSnapshot> snapshots = downloadAndSnapshot(vfsCtx, vfsPaths, tempDir);

            // 3. 映射路径并执行 Bash
            String mappedScript = mapPathsToLocal(script, vfsPaths, tempDir);
            String output = executeBash(mappedScript, tempDir);

            // 4. 同步修改回 VFS（双向同步）
            syncBackToVfs(vfsCtx, tempDir, snapshots);

            return output;

        } catch (IOException e) {
            throw new RuntimeException("vfsbash: execution failed", e);
        } finally {
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

    // ========== 路径提取 ==========

    /**
     * 提取脚本中引用的 VFS 路径
     */
    private Set<String> extractVfsPaths(String script, VfsContext vfsCtx) {
        Set<String> paths = new LinkedHashSet<>();
        Matcher matcher = VFS_PATH_PATTERN.matcher(script);

        while (matcher.find()) {
            String path = matcher.group(1);

            // 跳过常见的系统路径
            if (isSystemPath(path)) continue;

            // 检查 VFS 中是否存在
            if (Vfs.path(vfsCtx, path).exists()) {
                paths.add(path);
            }
        }

        return paths;
    }

    /**
     * 判断是否是系统路径（不需要 VFS 映射）
     */
    private boolean isSystemPath(String path) {
        return path.startsWith("/usr/")
            || path.startsWith("/bin/")
            || path.startsWith("/etc/")
            || path.startsWith("/tmp/")
            || path.startsWith("/dev/")
            || path.startsWith("/proc/")
            || path.startsWith("/var/");
    }

    // ========== 下载和快照 ==========

    /**
     * 流式下载 VFS 文件到临时目录，并记录文件快照
     */
    private Map<String, FileSnapshot> downloadAndSnapshot(
            VfsContext vfsCtx, Set<String> vfsPaths, Path tempDir) throws IOException {

        Map<String, FileSnapshot> snapshots = new HashMap<>();

        for (String vfsPath : vfsPaths) {
            if (Vfs.path(vfsCtx, vfsPath).isDirectory()) {
                downloadDirectory(vfsCtx, vfsPath, tempDir, snapshots);
            } else {
                downloadFile(vfsCtx, vfsPath, tempDir, snapshots);
            }
        }

        return snapshots;
    }

    /**
     * 流式下载单个文件
     */
    private void downloadFile(VfsContext vfsCtx, String vfsPath, Path tempDir,
                              Map<String, FileSnapshot> snapshots) throws IOException {
        Path localPath = tempDir.resolve(vfsPath.substring(1));
        Files.createDirectories(localPath.getParent());

        // 流式下载
        try (InputStream is = Vfs.path(vfsCtx, vfsPath).cat().execute()) {
            Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 记录快照
        snapshots.put(vfsPath, FileSnapshot.capture(localPath));
    }

    /**
     * 流式下载目录
     */
    private void downloadDirectory(VfsContext vfsCtx, String vfsPath, Path tempDir,
                                   Map<String, FileSnapshot> snapshots) throws IOException {
        // 创建本地目录
        Path localDir = tempDir.resolve(vfsPath.substring(1));
        Files.createDirectories(localDir);

        // 递归下载子文件
        List<VfsNode> nodes = Vfs.path(vfsCtx, vfsPath).ls();
        for (VfsNode node : nodes) {
            if (!Boolean.TRUE.equals(node.getIsDirectory())) {
                downloadFile(vfsCtx, node.getPath(), tempDir, snapshots);
            }
        }
    }

    // ========== 路径映射 ==========

    /**
     * 将脚本中的 VFS 路径映射到临时目录
     */
    private String mapPathsToLocal(String script, Set<String> vfsPaths, Path tempDir) {
        String mapped = script;
        for (String vfsPath : vfsPaths) {
            String localPath = tempDir.resolve(vfsPath.substring(1)).toString();
            mapped = mapped.replace(vfsPath, localPath);
        }
        return mapped;
    }

    // ========== 双向同步 ==========

    /**
     * 同步修改回 VFS（双向同步）
     * <p>
     * 1. 检测修改的文件，流式上传回 VFS
     * 2. 检测新创建的文件，流式上传到 VFS
     * 3. 检测删除的文件，从 VFS 删除
     */
    private void syncBackToVfs(VfsContext vfsCtx, Path tempDir,
                               Map<String, FileSnapshot> snapshots) throws IOException {

        // 检查已跟踪文件的修改和删除
        for (Map.Entry<String, FileSnapshot> entry : snapshots.entrySet()) {
            String vfsPath = entry.getKey();
            FileSnapshot snapshot = entry.getValue();
            Path localPath = tempDir.resolve(vfsPath.substring(1));

            if (!Files.exists(localPath)) {
                // 文件被删除，从 VFS 删除
                Vfs.path(vfsCtx, vfsPath).rm(false);
                continue;
            }

            if (snapshot.isModified(localPath)) {
                // 文件被修改，流式上传回 VFS
                try (InputStream input = Files.newInputStream(localPath)) {
                    Vfs.path(vfsCtx, vfsPath).write(input);
                }
            }
        }

        // 检测新创建的文件
        Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path localPath, BasicFileAttributes attrs) throws IOException {
                String relativePath = tempDir.relativize(localPath).toString();
                String vfsPath = "/" + relativePath;

                if (!snapshots.containsKey(vfsPath)) {
                    // 新文件，流式上传到 VFS
                    try (InputStream input = Files.newInputStream(localPath)) {
                        Vfs.path(vfsCtx, vfsPath).write(input);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ========== Bash 执行 ==========

    /**
     * 执行 Bash 命令
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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("vfsbash: execute failed", e);
        }
    }

    // ========== 清理 ==========

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
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

    // ========== 文件快照 ==========

    /**
     * 文件快照 - 用于检测文件修改
     */
    private static class FileSnapshot {
        private final long lastModified;
        private final long size;
        private final long checksum;

        FileSnapshot(long lastModified, long size, long checksum) {
            this.lastModified = lastModified;
            this.size = size;
            this.checksum = checksum;
        }

        static FileSnapshot capture(Path path) throws IOException {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return new FileSnapshot(
                attrs.lastModifiedTime().toMillis(),
                attrs.size(),
                calculateChecksum(path)
            );
        }

        boolean isModified(Path path) throws IOException {
            FileSnapshot current = capture(path);
            return current.lastModified != this.lastModified
                || current.size != this.size
                || current.checksum != this.checksum;
        }

        /**
         * CRC32 校验和（流式计算，不加载到内存）
         */
        private static long calculateChecksum(Path path) throws IOException {
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[8192];
            try (InputStream input = Files.newInputStream(path)) {
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    crc.update(buffer, 0, bytesRead);
                }
            }
            return crc.getValue();
        }
    }
}
