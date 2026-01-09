package com.dev.lib.storage.domain.service.virtual;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.dev.lib.storage.domain.service.virtual.VfsPathUtils.*;

@Service
@RequiredArgsConstructor
public class VirtualFileSystemImpl implements VirtualFileSystem {

    private final VfsInternalHelper helper;

    private static final int MAX_READ_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // ==================== 目录列表 ====================

    @Override
    public List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth) {

        if (depth == null || depth < 1) depth = 1;
        if (depth > 3) depth = 3;

        String fullPath = helper.resolvePath(ctx, path);

        if ("/".equals(fullPath) || fullPath.isEmpty()) {
            return helper.buildNodes(ctx, helper.findChildren(ctx, "/"), depth);
        }

        SysFile target = helper.findByPath(ctx, fullPath)
                .orElseThrow(() -> new IllegalArgumentException("Path not found: " + path));

        if (!Boolean.TRUE.equals(target.getIsDirectory())) {
            return List.of(helper.toNode(ctx, target, 0));
        }

        return helper.buildNodes(ctx, helper.findChildren(ctx, fullPath), depth);
    }

    // ==================== 文件读取 ====================

    @Override
    public InputStream openFile(VfsContext ctx, String path) {

        String fullPath = helper.resolvePath(ctx, path);
        SysFile file = helper.findByPath(ctx, fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot read directory: " + path);
        }

        try {
            return helper.getStorageService().download(file.getStoragePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public String readFile(VfsContext ctx, String path) {

        String fullPath = helper.resolvePath(ctx, path);
        SysFile file = helper.findByPath(ctx, fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (file.getSize() != null && file.getSize() == 0) {
            return "";
        }
        if (file.getSize() != null && file.getSize() > MAX_READ_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File too large (" + file.getSize() + " bytes). Use readLines() instead.");
        }

        try (InputStream is = openFile(ctx, path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount) {

        if (startLine < 1) {
            throw new IllegalArgumentException("Start line must be >= 1");
        }

        List<String> result = new ArrayList<>();
        try (InputStream is = openFile(ctx, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            for (int i = 1; i < startLine; i++) {
                if (reader.readLine() == null) return result;
            }

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                result.add(line);
                count++;
                if (lineCount != -1 && count >= lineCount) break;
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines from file", e);
        }
    }

    @Override
    public byte[] readBytes(VfsContext ctx, String path, long offset, int limit) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }

        String fullPath = helper.resolvePath(ctx, path);
        SysFile file = helper.findByPath(ctx, fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot read directory: " + path);
        }

        try (InputStream is = openFile(ctx, path)) {
            // 跳过 offset 字节
            long skipped = 0;
            while (skipped < offset) {
                long s = is.skip(offset - skipped);
                if (s == 0) break; // 已到达文件末尾
                skipped += s;
            }

            if (limit == -1) {
                return is.readAllBytes();
            }

            byte[] result = new byte[limit];
            int totalRead = 0;
            while (totalRead < limit) {
                int read = is.read(result, totalRead, limit - totalRead);
                if (read == -1) break;
                totalRead += read;
            }

            if (totalRead < limit) {
                byte[] trimmed = new byte[totalRead];
                System.arraycopy(result, 0, trimmed, 0, totalRead);
                return trimmed;
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bytes from file", e);
        }
    }

    @Override
    public long getFileSize(VfsContext ctx, String path) {

        String fullPath = helper.resolvePath(ctx, path);
        SysFile file = helper.findByPath(ctx, fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot get size of directory: " + path);
        }

        return file.getSize() != null ? file.getSize() : 0;
    }

    @Override
    public int getLineCount(VfsContext ctx, String path) {

        try (InputStream is = openFile(ctx, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            int count = 0;
            while (reader.readLine() != null) count++;
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to count lines in file", e);
        }
    }

    // ==================== 文件写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, String content) {

        byte[] bytes      = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        String fullPath   = helper.resolvePath(ctx, path);
        String parentPath = getParentPath(fullPath);

        if (parentPath != null && !"/".equals(parentPath) && helper.findByPath(ctx, parentPath).isEmpty()) {
            helper.ensureDirs(ctx, parentPath, new HashSet<>());
        }

        Optional<SysFile> existing = helper.findByPathForUpdate(ctx, fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot write to directory: " + path);
            }

            try (InputStream is = new ByteArrayInputStream(bytes)) {
                doWriteWithCOW(file, is, bytes.length, fullPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file", e);
            }
        } else {
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                helper.createFile(ctx, fullPath, is, bytes.length);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, InputStream inputStream) {

        String fullPath = helper.resolvePath(ctx, path);
        String parentPath = getParentPath(fullPath);

        if (parentPath != null && !"/".equals(parentPath) && helper.findByPath(ctx, parentPath).isEmpty()) {
            helper.ensureDirs(ctx, parentPath, new HashSet<>());
        }

        Optional<SysFile> existing = helper.findByPathForUpdate(ctx, fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot write to directory: " + path);
            }
            try {
                doWriteWithCOW(file, inputStream, -1, fullPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file", e);
            }
        } else {
            helper.createFile(ctx, fullPath, inputStream, -1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendFile(VfsContext ctx, String path, String content) {

        String fullPath   = helper.resolvePath(ctx, path);
        String parentPath = getParentPath(fullPath);

        if (parentPath != null && !"/".equals(parentPath) && helper.findByPath(ctx, parentPath).isEmpty()) {
            helper.ensureDirs(ctx, parentPath, new HashSet<>());
        }

        Optional<SysFile> existing = helper.findByPathForUpdate(ctx, fullPath);
        byte[] contentBytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot append to directory: " + path);
            }

            try {
                // 使用流式追加，避免大文件 OOM
                String newStoragePath = helper.appendAndUpload(file.getStoragePath(), contentBytes, getName(fullPath));

                String oldStoragePath = file.getStoragePath();
                file.setStoragePath(newStoragePath);
                file.setUrl(helper.getStorageService().getUrl(newStoragePath));
                file.setSize(file.getSize() == null ? contentBytes.length : file.getSize() + contentBytes.length);

                if (oldStoragePath != null && !oldStoragePath.equals(newStoragePath)) {
                    List<String> oldPaths = file.getOldStoragePaths();
                    if (oldPaths == null) {
                        oldPaths = new ArrayList<>();
                    }
                    if (oldPaths.size() >= 10) {
                        List<String> toDelete = oldPaths.subList(0, 5);
                        helper.getStorageService().deleteAll(new ArrayList<>(toDelete));
                        toDelete.clear();
                    }
                    oldPaths.add(oldStoragePath);
                    file.setOldStoragePaths(oldPaths);
                    file.setDeleteAfter(LocalDateTime.now().plusMinutes(5));
                }

                helper.getFileRepository().save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to append file", e);
            }
        } else {
            // 文件不存在，直接创建
            try (InputStream is = new ByteArrayInputStream(contentBytes)) {
                helper.createFile(ctx, fullPath, is, contentBytes.length);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file", e);
            }
        }
    }

    /**
     * 执行带 COW 的写入操作
     *
     * @param file 已存在的文件记录
     * @param contentStream 内容流
     * @param size 内容大小
     * @param fullPath 完整路径（用于获取文件名）
     */
    private void doWriteWithCOW(SysFile file, InputStream contentStream, long size, String fullPath) throws IOException {

        String oldStoragePath = file.getStoragePath();
        String newStoragePath = helper.uploadContent(contentStream, getName(fullPath));

        file.setStoragePath(newStoragePath);
        file.setUrl(helper.getStorageService().getUrl(newStoragePath));
        file.setSize(size);

        if (oldStoragePath != null && !oldStoragePath.equals(newStoragePath)) {
            List<String> oldPaths = file.getOldStoragePaths();
            if (oldPaths == null) {
                oldPaths = new ArrayList<>();
            }

            // 如果历史版本超过 10 个，删除最旧的 5 个
            if (oldPaths.size() >= 10) {
                List<String> toDelete = oldPaths.subList(0, 5);
                helper.getStorageService().deleteAll(new ArrayList<>(toDelete));
                toDelete.clear(); // subList.clear() 会从原 list 中移除
            }

            // 添加新的旧路径到末尾（FIFO）
            oldPaths.add(oldStoragePath);
            file.setOldStoragePaths(oldPaths);
            file.setDeleteAfter(LocalDateTime.now().plusMinutes(5));
        }

        helper.getFileRepository().save(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchFile(VfsContext ctx, String path) {

        String fullPath = helper.resolvePath(ctx, path);
        Optional<SysFile> existing = helper.findByPathForUpdate(ctx, fullPath);

        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot touch directory: " + path);
            }
            helper.getFileRepository().save(file);
        } else {
            try (InputStream is = new ByteArrayInputStream(new byte[0])) {
                helper.createFile(ctx, fullPath, is, 0);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create empty file", e);
            }
        }
    }

    // ==================== 移动操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(VfsContext ctx, String srcPath, String destPath) {

        String fullSrc  = helper.resolvePath(ctx, srcPath);
        String fullDest = helper.resolvePath(ctx, destPath);

        SysFile src = helper.findByPathForUpdate(ctx, fullSrc)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        Optional<SysFile> destOpt = helper.findByPathForUpdate(ctx, fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        if (helper.findByPathForUpdate(ctx, fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists: " + destPath);
        }

        if (Boolean.TRUE.equals(src.getIsDirectory()) && isSubPath(fullSrc, fullDest)) {
            throw new IllegalArgumentException("Cannot move directory into itself: " + srcPath + " -> " + destPath);
        }

        // 标记第 393-395 行是否已经处理过目录情况
        boolean destAlreadyHandledAsDir = destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory());

        String destParent = getParentPath(fullDest);
        if (destParent != null && !"/".equals(destParent) && helper.findByPathForUpdate(ctx, destParent).isEmpty()) {
            helper.ensureDirs(ctx, destParent, new HashSet<>());
        }

        // 如果 dest 已经在前面作为目录处理过（393-395），这里不再重复处理
        boolean moveToDir = (!destAlreadyHandledAsDir && destPath.endsWith("/")) ||
                (Boolean.FALSE.equals(src.getIsDirectory()) &&
                 destOpt.isEmpty() &&
                 destParent != null &&
                 !"/".equals(destParent) &&
                 helper.findByPathForUpdate(ctx, destParent).isPresent());

        if (moveToDir) {
            helper.ensureDirs(ctx, fullDest, new HashSet<>());
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            List<SysFile> descendants = helper.findDescendantsForUpdate(ctx, fullSrc + "/");
            for (SysFile desc : descendants) {
                String newPath = fullDest + desc.getVirtualPath().substring(fullSrc.length());
                desc.setVirtualPath(newPath);
                desc.setParentPath(getParentPath(newPath));
            }
            helper.getFileRepository().saveAll(descendants);
        }

        src.setVirtualPath(fullDest);
        src.setParentPath(getParentPath(fullDest));
        src.setOriginalName(getName(fullDest));
        helper.getFileRepository().save(src);
    }

    // ==================== 复制操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {

        String fullSrc  = helper.resolvePath(ctx, srcPath);
        String fullDest = helper.resolvePath(ctx, destPath);

        SysFile src = helper.findByPath(ctx, fullSrc)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            if (!recursive) {
                throw new IllegalArgumentException(
                        "Cannot copy directory without recursive flag. Use copy(ctx, src, dest, true)");
            }
            copyDirectoryRecursive(ctx, fullSrc, fullDest);
            return;
        }

        Optional<SysFile> destOpt = helper.findByPathForUpdate(ctx, fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        if (helper.findByPathForUpdate(ctx, fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists");
        }

        String destParent = getParentPath(fullDest);
        if (destParent != null && !"/".equals(destParent) && helper.findByPath(ctx, destParent).isEmpty()) {
            helper.ensureDirs(ctx, destParent, new HashSet<>());
        }

        copyFileRecord(src, fullDest);
    }

    private void copyDirectoryRecursive(VfsContext ctx, String srcDir, String destDir) {

        if (helper.findByPath(ctx, destDir).isEmpty()) {
            helper.ensureDirs(ctx, destDir, new HashSet<>());
        }

        List<SysFile> children = helper.findChildren(ctx, srcDir);
        for (SysFile child : children) {
            String childName = getName(child.getVirtualPath());
            String destPath  = destDir + "/" + childName;

            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                copyDirectoryRecursive(ctx, child.getVirtualPath(), destPath);
            } else {
                copyFileRecord(child, destPath);
            }
        }
    }

    private void copyFileRecord(SysFile src, String destPath) {

        // 复制物理文件，避免共享 storagePath 导致悬空引用
        String newStoragePath = null;
        if (src.getStoragePath() != null) {
            try (InputStream is = helper.getStorageService().download(src.getStoragePath())) {
                newStoragePath = helper.uploadContent(is, getName(destPath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy file content", e);
            }
        }

        SysFile copy = new SysFile();
        copy.setBizId(IDWorker.newId());
        copy.setVirtualPath(destPath);
        copy.setParentPath(getParentPath(destPath));
        copy.setIsDirectory(false);
        copy.setOriginalName(getName(destPath));
        copy.setStorageName(src.getStorageName());
        copy.setStoragePath(newStoragePath);
        copy.setUrl(newStoragePath != null ? helper.getStorageService().getUrl(newStoragePath) : null);
        copy.setExtension(src.getExtension());
        copy.setContentType(src.getContentType());
        copy.setSize(src.getSize());
        copy.setStorageType(src.getStorageType());
        helper.getFileRepository().save(copy);
    }

    // ==================== 删除操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(VfsContext ctx, String path, boolean recursive) {

        String fullPath = helper.resolvePath(ctx, path);
        Optional<SysFile> opt = helper.findByPathForUpdate(ctx, fullPath);

        if (opt.isEmpty()) {
            if (recursive) return;
            throw new IllegalArgumentException("Path not found: " + path);
        }

        SysFile file = opt.get();
        List<String> storagePathsToDelete = new ArrayList<>();

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            List<SysFile> children = helper.findChildren(ctx, fullPath);
            if (!children.isEmpty() && !recursive) {
                throw new IllegalArgumentException("Directory not empty (use recursive=true to force delete): " + path);
            }
            if (recursive) {
                List<SysFile> descendants = helper.findDescendants(ctx, fullPath + "/");
                // 收集所有需要删除的物理文件路径
                for (SysFile desc : descendants) {
                    collectStoragePaths(desc, storagePathsToDelete);
                }
                helper.getFileRepository().deleteAll(descendants);
            }
        } else {
            // 收集当前文件的物理路径
            collectStoragePaths(file, storagePathsToDelete);
        }

        helper.getFileRepository().delete(file);

        // 删除物理文件
        if (!storagePathsToDelete.isEmpty()) {
            helper.getStorageService().deleteAll(storagePathsToDelete);
        }
    }

    /**
     * 收集文件的所有存储路径（当前路径 + 历史版本）
     */
    private void collectStoragePaths(SysFile file, List<String> paths) {
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            return;
        }
        if (file.getStoragePath() != null) {
            paths.add(file.getStoragePath());
        }
        if (file.getOldStoragePaths() != null) {
            paths.addAll(file.getOldStoragePaths());
        }
    }

    // ==================== 目录创建 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDirectory(VfsContext ctx, String path, boolean createParents) {

        String fullPath = helper.resolvePath(ctx, path);

        if (helper.findByPath(ctx, fullPath).isPresent()) {
            if (createParents) return;
            throw new IllegalArgumentException("Path already exists: " + path);
        }

        if (createParents) {
            helper.ensureDirs(ctx, fullPath, new HashSet<>());
        } else {
            String parentPath = getParentPath(fullPath);
            if (parentPath != null && !"/".equals(parentPath) && helper.findByPath(ctx, parentPath).isEmpty()) {
                throw new IllegalArgumentException("Parent directory not found");
            }
            helper.createDirectoryInternal(fullPath);
        }
    }

    // ==================== 查找操作 ====================

    @Override
    public List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {

        String fullBasePath = helper.resolvePath(ctx, basePath);

        SysFile base = helper.findByPath(ctx, fullBasePath)
                .orElseThrow(() -> new IllegalArgumentException("Base path not found: " + basePath));

        if (!Boolean.TRUE.equals(base.getIsDirectory())) {
            throw new IllegalArgumentException("Base path is not a directory: " + basePath);
        }

        List<VfsNode> results = new ArrayList<>();
        String baseName = getName(fullBasePath);
        if (baseName != null && matchPattern(baseName, pattern)) {
            results.add(helper.toNode(ctx, base, 0));
        }

        findRecursive(ctx, fullBasePath, pattern, recursive, results);
        return results;
    }

    private void findRecursive(VfsContext ctx, String currentPath, String pattern, boolean recursive, List<VfsNode> results) {

        List<SysFile> children = helper.findChildren(ctx, currentPath);
        for (SysFile child : children) {
            String name = getName(child.getVirtualPath());
            if (matchPattern(name, pattern)) {
                results.add(helper.toNode(ctx, child, 0));
            }
            if (recursive && Boolean.TRUE.equals(child.getIsDirectory())) {
                findRecursive(ctx, child.getVirtualPath(), pattern, true, results);
            }
        }
    }

    @Override
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {

        String fullBasePath = helper.resolvePath(ctx, basePath);

        SysFile base = helper.findByPath(ctx, fullBasePath)
                .orElseThrow(() -> new IllegalArgumentException("Base path not found: " + basePath));

        if (!Boolean.TRUE.equals(base.getIsDirectory())) {
            throw new IllegalArgumentException("Base path is not a directory: " + basePath);
        }

        List<VfsNode> results = new ArrayList<>();
        grepRecursive(ctx, fullBasePath, content, recursive, results);
        return results;
    }

    private void grepRecursive(VfsContext ctx, String currentPath, String searchContent, boolean recursive, List<VfsNode> results) {

        List<SysFile> children = helper.findChildren(ctx, currentPath);
        for (SysFile child : children) {
            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                if (recursive) {
                    grepRecursive(ctx, child.getVirtualPath(), searchContent, true, results);
                }
            } else {
                try (InputStream is = openFile(ctx, child.getVirtualPath());
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String line;
                    boolean found = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(searchContent)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        results.add(helper.toNode(ctx, child, 0));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ==================== 上传操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {

        String basePath = helper.resolvePath(ctx, path);

        if (!"/".equals(basePath) && helper.findByPath(ctx, basePath).isEmpty()) {
            helper.ensureDirs(ctx, basePath, new HashSet<>());
        }

        List<String> fileIds = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            Set<String> createdDirs = new HashSet<>();
            createdDirs.add(basePath);

            while ((entry = zis.getNextEntry()) != null) {
                String entryPath = normalizePath(basePath + "/" + entry.getName());

                if (entry.isDirectory()) {
                    if (!createdDirs.contains(entryPath) && helper.findByPath(ctx, entryPath).isEmpty()) {
                        helper.ensureDirs(ctx, entryPath, createdDirs);
                    }
                } else {
                    String parentPath = getParentPath(entryPath);
                    if (parentPath != null && !"/".equals(parentPath) && !createdDirs.contains(parentPath)
                            && helper.findByPath(ctx, parentPath).isEmpty()) {
                        helper.ensureDirs(ctx, parentPath, createdDirs);
                    }

                    if (helper.findByPath(ctx, entryPath).isPresent()) {
                        throw new IllegalArgumentException("File already exists: " + entryPath);
                    }
                    String bizId = helper.createFile(ctx, entryPath, zis, entry.getSize());
                    fileIds.add(bizId);
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract zip", e);
        }
        return fileIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths) {

        String basePath = helper.resolvePath(ctx, targetPath);

        if (!"/".equals(basePath) && helper.findByPath(ctx, basePath).isEmpty()) {
            helper.ensureDirs(ctx, basePath, new HashSet<>());
        }

        Set<String> createdDirs = new HashSet<>();
        createdDirs.add(basePath);
        List<String> fileIds = new ArrayList<>();

        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String relativePath = relativePaths != null && i < relativePaths.length
                        ? relativePaths[i] : file.getOriginalFilename();

                if (relativePath == null || relativePath.isEmpty()) continue;

                String fullPath = normalizePath(basePath + "/" + relativePath);
                String parentPath = getParentPath(fullPath);

                if (parentPath != null && !"/".equals(parentPath) && !createdDirs.contains(parentPath)) {
                    if (helper.findByPath(ctx, parentPath).isEmpty()) {
                        helper.ensureDirs(ctx, parentPath, createdDirs);
                    } else {
                        createdDirs.add(parentPath);
                    }
                }

                if (helper.findByPath(ctx, fullPath).isPresent()) {
                    throw new IllegalArgumentException("File already exists: " + fullPath);
                }
                String bizId = helper.createFile(ctx, fullPath, file.getInputStream(), file.getSize());
                fileIds.add(bizId);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload files", e);
        }
        return fileIds;
    }

    // ==================== 工具方法 ====================

    @Override
    public boolean exists(VfsContext ctx, String path) {
        return helper.findByPath(ctx, helper.resolvePath(ctx, path)).isPresent();
    }

    @Override
    public boolean isDirectory(VfsContext ctx, String path) {
        return helper.findByPath(ctx, helper.resolvePath(ctx, path))
                .map(f -> Boolean.TRUE.equals(f.getIsDirectory()))
                .orElse(false);
    }
}
