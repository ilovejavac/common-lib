package com.dev.lib.storage.domain.service;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class VirtualFileSystemImpl implements VirtualFileSystem {

    private final SysFileRepository fileRepository;

    private final FileSystemRepository systemRepository;

    private final StorageService storageService;

    private final AppStorageProperties storageProperties;

    // ==================== 路径工具 ====================

    private String resolvePath(VfsContext ctx, String path) {

        String root = ctx.getRoot();
        if (root == null) root = "";
        if (path == null) path = "";

        root = normalizePath(root);
        path = normalizePath(path);

        if (path.isEmpty()) {
            return root.isEmpty() ? "/" : root;
        }
        if (root.isEmpty()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        return root + (path.startsWith("/") ? path : "/" + path);
    }

    private String normalizePath(String path) {

        if (path == null || path.isEmpty()) return "";

        String[]      parts = path.split("/");
        Deque<String> stack = new ArrayDeque<>();

        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (!stack.isEmpty()) stack.pollLast();
            } else {
                stack.addLast(part);
            }
        }
        // 根路径 / 应返回 / 而不是空字符串
        return stack.isEmpty() ? "/" : "/" + String.join("/", stack);
    }

    private String getParentPath(String virtualPath) {

        if (virtualPath == null || "/".equals(virtualPath)) return null;
        int idx = virtualPath.lastIndexOf('/');
        return idx <= 0 ? "/" : virtualPath.substring(0, idx);
    }

    private String getName(String virtualPath) {

        if (virtualPath == null) return null;
        int idx = virtualPath.lastIndexOf('/');
        return idx < 0 ? virtualPath : virtualPath.substring(idx + 1);
    }

    /**
     * 检查 child 是否是 parent 的子路径（或相同）
     */
    private boolean isSubPath(String parent, String child) {

        if (parent == null || child == null) return false;
        if (parent.equals(child)) return true;
        String prefix = parent.endsWith("/") ? parent : parent + "/";
        return child.startsWith(prefix);
    }

    /**
     * 通配符模式匹配（支持 * 和 ?）
     * 转义顺序很重要：先转义 .，再处理 * 和 ?
     */
    private boolean matchPattern(String name, String pattern) {

        if (pattern == null || pattern.isEmpty()) return true;

        // 转换通配符为正则表达式
        // 注意：必须先转义 .，否则后面生成的 .* 中的 . 也会被转义
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '.':
                    regex.append("\\.");  // 转义字面量 .
                    break;
                case '*':
                    regex.append(".*");   // * → .*
                    break;
                case '?':
                    regex.append(".");    // ? → .
                    break;
                // 其他正则元字符也需要转义
                case '^':
                case '$':
                case '+':
                case '|':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
            }
        }

        return Pattern.matches(regex.toString(), name);
    }

    // ==================== 查询（使用悲观锁的版本） ====================

    private Optional<SysFile> findByPath(VfsContext ctx, String virtualPath) {

        return systemRepository.findByVirtualPath(virtualPath);
    }

    /**
     * 悲观锁查询，用于写操作
     */
    private Optional<SysFile> findByPathForUpdate(VfsContext ctx, String virtualPath) {

        return systemRepository.findByVirtualPathForUpdate(virtualPath);
    }

    private List<SysFile> findChildren(VfsContext ctx, String parentPath) {

        return systemRepository.findByParentPath(parentPath);
    }

    private List<SysFile> findDescendants(VfsContext ctx, String prefix) {

        return systemRepository.findByVirtualPathStartingWith(prefix);
    }

    // ==================== 目录创建（统一方法） ====================

    /**
     * 递归创建目录（内部方法，直接用完整路径）
     * 使用数据库唯一约束来防止并发创建重复目录
     */
    private void ensureDirs(VfsContext ctx, String fullPath, Set<String> created) {

        if (fullPath == null || "/".equals(fullPath) || created.contains(fullPath)) {
            return;
        }
        if (findByPath(ctx, fullPath).isPresent()) {
            return;
        }
        // 先创建父目录
        String parent = getParentPath(fullPath);
        if (parent != null && !"/".equals(parent)) {
            ensureDirs(ctx, parent, created);
        }
        // 再创建当前目录（可能因为并发而失败，忽略唯一约束异常）
        try {
            createDirectory(ctx, fullPath);
            created.add(fullPath);
        } catch (DataIntegrityViolationException e) {
            // 并发创建导致的唯一约束冲突，验证目录是否真的存在
            if (findByPath(ctx, fullPath).isPresent()) {
                created.add(fullPath);
            } else {
                // 目录不存在，说明是其他异常，重新抛出
                throw new RuntimeException("Failed to create directory: " + fullPath, e);
            }
        }
    }

    private void createDirectory(VfsContext ctx, String virtualPath) {

        SysFile dir = new SysFile();
        dir.setBizId(IDWorker.newId());
        dir.setVirtualPath(virtualPath);
        dir.setParentPath(getParentPath(virtualPath));
        dir.setIsDirectory(true);
        dir.setOriginalName(getName(virtualPath));
        dir.setStorageName(getName(virtualPath));
        fileRepository.save(dir);
    }

    private String createFile(VfsContext ctx, String virtualPath, byte[] content) {

        String fileName  = getName(virtualPath);
        String extension = "";
        int    dotIdx    = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = fileName.substring(dotIdx + 1).toLowerCase();
        }

        try {
            String storagePath = uploadContent(content, fileName);

            SysFile file  = new SysFile();
            String  bizId = IDWorker.newId();
            file.setBizId(bizId);
            file.setVirtualPath(virtualPath);
            file.setParentPath(getParentPath(virtualPath));
            file.setIsDirectory(false);
            file.setOriginalName(fileName);
            file.setStorageName(fileName);
            file.setStoragePath(storagePath);
            file.setUrl(storageService.getUrl(storagePath));
            file.setExtension(extension);
            file.setSize((long) content.length);
            file.setStorageType(storageProperties.getType());
            fileRepository.save(file);
            return bizId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    private String createFile(VfsContext ctx, String virtualPath, InputStream inputStream, long size) {

        String fileName  = getName(virtualPath);
        String extension = "";
        int    dotIdx    = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = fileName.substring(dotIdx + 1).toLowerCase();
        }

        try {
            String storagePath = uploadContent(inputStream, fileName, size);

            SysFile file  = new SysFile();
            String  bizId = IDWorker.newId();
            file.setBizId(bizId);
            file.setVirtualPath(virtualPath);
            file.setParentPath(getParentPath(virtualPath));
            file.setIsDirectory(false);
            file.setOriginalName(fileName);
            file.setStorageName(fileName);
            file.setStoragePath(storagePath);
            file.setUrl(storageService.getUrl(storagePath));
            file.setExtension(extension);
            file.setSize(size);
            file.setStorageType(storageProperties.getType());
            fileRepository.save(file);
            return bizId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    private String uploadContent(byte[] content, String fileName) throws IOException {

        String storageName = IDWorker.newId();
        int    dotIdx      = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            storageName += fileName.substring(dotIdx);
        }

        LocalDate now = LocalDate.now();
        String storagePath = String.format(
                "vfs/%d/%02d/%02d/%s",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                storageName
        );

        storageService.upload(new ByteArrayInputStream(content), storagePath);
        return storagePath;
    }

    private String uploadContent(InputStream inputStream, String fileName, long size) throws IOException {

        String storageName = IDWorker.newId();
        int    dotIdx      = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            storageName += fileName.substring(dotIdx);
        }

        LocalDate now = LocalDate.now();
        String storagePath = String.format(
                "vfs/%d/%02d/%02d/%s",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                storageName
        );

        storageService.upload(inputStream, storagePath);
        return storagePath;
    }

    // ==================== 接口实现 ====================

    @Override
    public List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth) {

        if (depth == null || depth < 1) depth = 1;
        if (depth > 3) depth = 3;

        String fullPath = resolvePath(ctx, path);

        if ("/".equals(fullPath) || fullPath.isEmpty()) {
            return buildNodes(ctx, findChildren(ctx, "/"), depth);
        }

        SysFile target = findByPath(
                ctx,
                fullPath
        ).orElseThrow(() -> new IllegalArgumentException("Path not found: " + path));

        if (!Boolean.TRUE.equals(target.getIsDirectory())) {
            return List.of(toNode(ctx, target, 0));
        }

        return buildNodes(ctx, findChildren(ctx, fullPath), depth);
    }

    private List<VfsNode> buildNodes(VfsContext ctx, List<SysFile> files, int depth) {

        List<VfsNode> nodes = new ArrayList<>();
        for (SysFile file : files) {
            nodes.add(toNode(ctx, file, depth - 1));
        }
        return nodes;
    }

    private VfsNode toNode(VfsContext ctx, SysFile file, int remainingDepth) {

        VfsNode node = new VfsNode();
        node.setId(file.getBizId());
        node.setName(getName(file.getVirtualPath()));
        node.setPath(file.getVirtualPath());
        node.setIsDirectory(file.getIsDirectory());
        node.setSize(file.getSize());
        node.setExtension(file.getExtension());
        node.setModifiedAt(file.getUpdatedAt());

        if (Boolean.TRUE.equals(file.getIsDirectory()) && remainingDepth > 0) {
            node.setChildren(buildNodes(ctx, findChildren(ctx, file.getVirtualPath()), remainingDepth));
        }
        return node;
    }

    @Override
    public InputStream openFile(VfsContext ctx, String path) {

        String fullPath = resolvePath(ctx, path);
        SysFile file = findByPath(
                ctx,
                fullPath
        ).orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot read directory: " + path);
        }

        try {
            return storageService.download(file.getStoragePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public String readFile(VfsContext ctx, String path) {

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

            // 跳过前面的行
            for (int i = 1; i < startLine; i++) {
                if (reader.readLine() == null) {
                    // 起始行超出文件范围
                    return result;
                }
            }

            // 读取指定数量的行
            String line;
            int    count = 0;
            while ((line = reader.readLine()) != null) {
                result.add(line);
                count++;
                if (lineCount != -1 && count >= lineCount) {
                    break;
                }
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines from file", e);
        }
    }

    @Override
    public int getLineCount(VfsContext ctx, String path) {

        try (InputStream is = openFile(ctx, path);
             java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {

            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to count lines in file", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, String content) {

        writeFile(ctx, path, content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, byte[] content) {

        String fullPath   = resolvePath(ctx, path);
        String parentPath = getParentPath(fullPath);

        // 确保父目录存在
        if (parentPath != null && !"/".equals(parentPath) && findByPath(ctx, parentPath).isEmpty()) {
            ensureDirs(ctx, parentPath, new HashSet<>());
        }

        // 使用悲观锁查询，防止并发写
        Optional<SysFile> existing = findByPathForUpdate(ctx, fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot write to directory: " + path);
            }

            // Copy-on-Write 策略：
            // 1. 先上传新文件到存储（此时读操作仍然读取旧文件）
            // 2. 在事务中原子性地更新数据库记录的 storagePath
            // 3. 标记旧文件延迟删除
            try {
                String oldStoragePath = file.getStoragePath();
                String newStoragePath = uploadContent(content, getName(fullPath));

                // 原子性地替换指针（COW 的核心）
                file.setStoragePath(newStoragePath);
                file.setUrl(storageService.getUrl(newStoragePath));
                file.setSize((long) content.length);

                // 标记旧文件延迟删除（5分钟后）
                if (oldStoragePath != null && !oldStoragePath.equals(newStoragePath)) {
                    file.setOldStoragePath(oldStoragePath);
                    file.setTemporary(true);
                    file.setDeleteAfter(LocalDateTime.now().plusMinutes(5));
                }

                // 保存更新（不创建新记录，只更新现有记录）
                fileRepository.save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file", e);
            }
        } else {
            // 文件不存在，创建新文件
            createFile(ctx, fullPath, content);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, InputStream inputStream) {

        try {
            byte[] content = inputStream.readAllBytes();
            writeFile(ctx, path, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from input stream", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchFile(VfsContext ctx, String path) {

        String fullPath = resolvePath(ctx, path);

        Optional<SysFile> existing = findByPathForUpdate(ctx, fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot touch directory: " + path);
            }
            // 更新时间戳（JPA 会自动更新 updatedAt）
            fileRepository.save(file);
        } else {
            // 创建空文件
            writeFile(ctx, path, new byte[0]);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(VfsContext ctx, String srcPath, String destPath) {

        String fullSrc  = resolvePath(ctx, srcPath);
        String fullDest = resolvePath(ctx, destPath);

        // 使用悲观锁锁定源文件
        SysFile src = findByPathForUpdate(
                ctx,
                fullSrc
        ).orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        // 目标是已存在目录则移入
        Optional<SysFile> destOpt = findByPathForUpdate(ctx, fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        // 检查目标是否已存在（使用悲观锁，在环检测之前检查）
        if (findByPathForUpdate(ctx, fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists: " + destPath);
        }

        // 环检测：不能把父目录移到子目录（必须在目标重计算之后进行）
        if (Boolean.TRUE.equals(src.getIsDirectory()) && isSubPath(fullSrc, fullDest)) {
            throw new IllegalArgumentException("Cannot move directory into itself: " + srcPath + " -> " + destPath);
        }

        // 确保目标父目录存在
        String destParent = getParentPath(fullDest);
        if (destParent != null && !"/".equals(destParent) && findByPath(ctx, destParent).isEmpty()) {
            ensureDirs(ctx, destParent, new HashSet<>());
        }

        // 如果目标不存在，且用户输入的 destPath 以 / 结尾，说明想移入目录
        // 或者：源是文件，目标不存在，且目标的父目录存在，则把目标当作目录创建
        boolean moveToDir = destPath.endsWith("/") ||
                (Boolean.FALSE.equals(src.getIsDirectory()) &&
                 !destOpt.isPresent() &&
                 destParent != null &&
                 !"/".equals(destParent) &&
                 findByPath(ctx, destParent).isPresent());

        if (moveToDir) {
            // 目标作为目录，创建它并移入
            String dirPath = fullDest;
            ensureDirs(ctx, dirPath, new HashSet<>());
            fullDest = dirPath + "/" + getName(fullSrc);
        }

        // 移动目录时更新所有子项（使用悲观锁锁定所有子项，防止并发修改）
        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            // 使用悲观锁查询所有子项
            List<SysFile> descendants = systemRepository.findByVirtualPathStartingWithForUpdate(fullSrc + "/");
            for (SysFile desc : descendants) {
                String newPath = fullDest + desc.getVirtualPath().substring(fullSrc.length());
                desc.setVirtualPath(newPath);
                desc.setParentPath(getParentPath(newPath));
            }
            fileRepository.saveAll(descendants);
        }

        src.setVirtualPath(fullDest);
        src.setParentPath(getParentPath(fullDest));
        src.setOriginalName(getName(fullDest));
        fileRepository.save(src);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {

        String fullSrc  = resolvePath(ctx, srcPath);
        String fullDest = resolvePath(ctx, destPath);

        SysFile src = findByPath(
                ctx,
                fullSrc
        ).orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            if (!recursive) {
                throw new IllegalArgumentException(
                        "Cannot copy directory without recursive flag. Use copy(ctx, src, dest, true)");
            }
            copyDirectoryRecursive(ctx, fullSrc, fullDest);
            return;
        }

        // 目标是已存在目录则复制到该目录下
        Optional<SysFile> destOpt = findByPathForUpdate(ctx, fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        if (findByPathForUpdate(ctx, fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists");
        }

        // 确保目标父目录存在
        String destParent = getParentPath(fullDest);
        if (destParent != null && !"/".equals(destParent) && findByPath(ctx, destParent).isEmpty()) {
            ensureDirs(ctx, destParent, new HashSet<>());
        }

        // 真正复制文件内容（修复 bug）
        try {
            byte[] content        = storageService.download(src.getStoragePath()).readAllBytes();
            String newStoragePath = uploadContent(content, getName(fullDest));

            SysFile copy = new SysFile();
            copy.setBizId(IDWorker.newId());
            copy.setVirtualPath(fullDest);
            copy.setParentPath(getParentPath(fullDest));
            copy.setIsDirectory(false);
            copy.setOriginalName(getName(fullDest));
            copy.setStorageName(src.getStorageName());
            copy.setStoragePath(newStoragePath);  // 使用新的存储路径
            copy.setUrl(storageService.getUrl(newStoragePath));
            copy.setExtension(src.getExtension());
            copy.setContentType(src.getContentType());
            copy.setSize(src.getSize());
            copy.setStorageType(src.getStorageType());
            fileRepository.save(copy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file", e);
        }
    }

    /**
     * 递归复制目录
     */
    private void copyDirectoryRecursive(VfsContext ctx, String srcDir, String destDir) {

        // 创建目标目录
        if (findByPath(ctx, destDir).isEmpty()) {
            ensureDirs(ctx, destDir, new HashSet<>());
        }

        // 获取源目录的所有子项
        List<SysFile> children = findChildren(ctx, srcDir);

        for (SysFile child : children) {
            String childName = getName(child.getVirtualPath());
            String destPath  = destDir + "/" + childName;

            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                // 递归复制子目录
                copyDirectoryRecursive(ctx, child.getVirtualPath(), destPath);
            } else {
                // 复制文件
                try {
                    byte[] content        = storageService.download(child.getStoragePath()).readAllBytes();
                    String newStoragePath = uploadContent(content, childName);

                    SysFile copy = new SysFile();
                    copy.setBizId(IDWorker.newId());
                    copy.setVirtualPath(destPath);
                    copy.setParentPath(destDir);
                    copy.setIsDirectory(false);
                    copy.setOriginalName(childName);
                    copy.setStorageName(child.getStorageName());
                    copy.setStoragePath(newStoragePath);
                    copy.setUrl(storageService.getUrl(newStoragePath));
                    copy.setExtension(child.getExtension());
                    copy.setContentType(child.getContentType());
                    copy.setSize(child.getSize());
                    copy.setStorageType(child.getStorageType());
                    fileRepository.save(copy);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy file: " + child.getVirtualPath(), e);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(VfsContext ctx, String path, boolean recursive) {

        String            fullPath = resolvePath(ctx, path);
        Optional<SysFile> opt      = findByPathForUpdate(ctx, fullPath);

        if (opt.isEmpty()) {
            if (recursive) {
                // rm -rf 时，文件不存在不报错
                return;
            }
            throw new IllegalArgumentException("Path not found: " + path);
        }

        SysFile file = opt.get();

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            List<SysFile> children = findChildren(ctx, fullPath);

            if (!children.isEmpty() && !recursive) {
                throw new IllegalArgumentException("Directory not empty (use recursive=true to force delete): " + path);
            }

            if (recursive) {
                // 递归删除所有子项
                List<SysFile> descendants = findDescendants(ctx, fullPath + "/");
                fileRepository.deleteAll(descendants);
            }
        }

        fileRepository.delete(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDirectory(VfsContext ctx, String path, boolean createParents) {

        String fullPath = resolvePath(ctx, path);

        if (findByPath(ctx, fullPath).isPresent()) {
            if (createParents) {
                // mkdir -p 时，目录已存在不报错
                return;
            }
            throw new IllegalArgumentException("Path already exists: " + path);
        }

        if (createParents) {
            // 递归创建父目录
            ensureDirs(ctx, fullPath, new HashSet<>());
        } else {
            // 只创建当前目录，父目录必须存在
            String parentPath = getParentPath(fullPath);
            if (parentPath != null && !"/".equals(parentPath) && findByPath(ctx, parentPath).isEmpty()) {
                throw new IllegalArgumentException("Parent directory not found");
            }
            createDirectory(ctx, fullPath);
        }
    }

    @Override
    public List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {

        String fullBasePath = resolvePath(ctx, basePath);

        Optional<SysFile> baseOpt = findByPath(ctx, fullBasePath);
        if (baseOpt.isEmpty()) {
            throw new IllegalArgumentException("Base path not found: " + basePath);
        }

        SysFile base = baseOpt.get();
        if (!Boolean.TRUE.equals(base.getIsDirectory())) {
            throw new IllegalArgumentException("Base path is not a directory: " + basePath);
        }

        List<VfsNode> results = new ArrayList<>();

        // 检查 basePath 本身是否匹配 pattern
        String baseName = getName(fullBasePath);
        if (baseName != null && matchPattern(baseName, pattern)) {
            results.add(toNode(ctx, base, 0));
        }

        // 递归搜索子项
        findRecursive(ctx, fullBasePath, pattern, recursive, results);
        return results;
    }

    private void findRecursive(VfsContext ctx, String currentPath, String pattern, boolean recursive, List<VfsNode> results) {

        List<SysFile> children = findChildren(ctx, currentPath);

        for (SysFile child : children) {
            String name = getName(child.getVirtualPath());

            // 检查是否匹配模式
            if (matchPattern(name, pattern)) {
                results.add(toNode(ctx, child, 0));
            }

            // 递归搜索子目录
            if (recursive && Boolean.TRUE.equals(child.getIsDirectory())) {
                findRecursive(ctx, child.getVirtualPath(), pattern, true, results);
            }
        }
    }

    @Override
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {

        String fullBasePath = resolvePath(ctx, basePath);

        Optional<SysFile> baseOpt = findByPath(ctx, fullBasePath);
        if (baseOpt.isEmpty()) {
            throw new IllegalArgumentException("Base path not found: " + basePath);
        }

        SysFile base = baseOpt.get();
        if (!Boolean.TRUE.equals(base.getIsDirectory())) {
            throw new IllegalArgumentException("Base path is not a directory: " + basePath);
        }

        List<VfsNode> results = new ArrayList<>();
        grepRecursive(ctx, fullBasePath, content, recursive, results);
        return results;
    }

    private void grepRecursive(VfsContext ctx, String currentPath, String searchContent, boolean recursive, List<VfsNode> results) {

        List<SysFile> children = findChildren(ctx, currentPath);

        for (SysFile child : children) {
            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                // 递归搜索子目录
                if (recursive) {
                    grepRecursive(ctx, child.getVirtualPath(), searchContent, true, results);
                }
            } else {
                // 逐行流式搜索文件内容（避免 OOM）
                try (InputStream is = openFile(ctx, child.getVirtualPath());
                     java.io.BufferedReader reader = new java.io.BufferedReader(
                             new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String  line;
                    boolean found = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(searchContent)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        results.add(toNode(ctx, child, 0));
                    }
                } catch (Exception e) {
                    // 忽略无法读取的文件
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {

        String basePath = resolvePath(ctx, path);

        // 确保目标目录存在
        if (!"/".equals(basePath) && findByPath(ctx, basePath).isEmpty()) {
            ensureDirs(ctx, basePath, new HashSet<>());
        }

        List<String> fileIds = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry    entry;
            Set<String> createdDirs = new HashSet<>();
            createdDirs.add(basePath);

            while ((entry = zis.getNextEntry()) != null) {
                String entryPath = normalizePath(basePath + "/" + entry.getName());

                if (entry.isDirectory()) {
                    if (!createdDirs.contains(entryPath) && findByPath(ctx, entryPath).isEmpty()) {
                        ensureDirs(ctx, entryPath, createdDirs);
                    }
                } else {
                    // 确保父目录存在
                    String parentPath = getParentPath(entryPath);
                    if (parentPath != null && !"/".equals(parentPath) && !createdDirs.contains(parentPath) && findByPath(
                            ctx,
                            parentPath
                    ).isEmpty()) {
                        ensureDirs(ctx, parentPath, createdDirs);
                    }

                    // 检查文件是否已存在（同步 Linux 行为：已存在则报错）
                    long size = entry.getSize();
                    if (findByPath(ctx, entryPath).isPresent()) {
                        throw new IllegalArgumentException("File already exists: " + entryPath);
                    }
                    String bizId = createFile(ctx, entryPath, zis, size);
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

        String basePath = resolvePath(ctx, targetPath);

        // 确保目标目录存在
        if (!"/".equals(basePath) && findByPath(ctx, basePath).isEmpty()) {
            ensureDirs(ctx, basePath, new HashSet<>());
        }

        Set<String> createdDirs = new HashSet<>();
        createdDirs.add(basePath);
        List<String> fileIds = new ArrayList<>();

        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file         = files[i];
                String        relativePath = relativePaths != null && i < relativePaths.length
                                             ? relativePaths[i]
                                             : file.getOriginalFilename();

                if (relativePath == null || relativePath.isEmpty()) {
                    continue;
                }

                // 构建完整的虚拟路径
                String fullPath = normalizePath(basePath + "/" + relativePath);

                // 确保父目录存在
                String parentPath = getParentPath(fullPath);
                if (parentPath != null && !"/".equals(parentPath) && !createdDirs.contains(parentPath)) {
                    if (findByPath(ctx, parentPath).isEmpty()) {
                        ensureDirs(ctx, parentPath, createdDirs);
                    } else {
                        createdDirs.add(parentPath);
                    }
                }

                // 检查文件是否已存在（同步 Linux 行为：已存在则报错）
                if (findByPath(ctx, fullPath).isPresent()) {
                    throw new IllegalArgumentException("File already exists: " + fullPath);
                }
                String bizId = createFile(ctx, fullPath, file.getInputStream(), file.getSize());
                fileIds.add(bizId);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload files", e);
        }

        return fileIds;
    }

    @Override
    public boolean exists(VfsContext ctx, String path) {

        return findByPath(ctx, resolvePath(ctx, path)).isPresent();
    }

    @Override
    public boolean isDirectory(VfsContext ctx, String path) {

        return findByPath(ctx, resolvePath(ctx, path)).map(f -> Boolean.TRUE.equals(f.getIsDirectory())).orElse(false);
    }

}
