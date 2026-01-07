package com.dev.lib.storage.domain.service;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
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
        return stack.isEmpty() ? "" : "/" + String.join("/", stack);
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

    // ==================== 查询 ====================

    private Optional<SysFile> findByPath(VfsContext ctx, String virtualPath) {

        return systemRepository.findByVirtualPath(virtualPath);
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
        // 再创建当前目录
        createDirectory(ctx, fullPath);
        created.add(fullPath);
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

    private void createFile(VfsContext ctx, String virtualPath, byte[] content) {

        String fileName  = getName(virtualPath);
        String extension = "";
        int    dotIdx    = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = fileName.substring(dotIdx + 1).toLowerCase();
        }

        try {
            String storagePath = uploadContent(content, fileName);

            SysFile file = new SysFile();
            file.setBizId(IDWorker.newId());
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

    // ==================== 接口实现 ====================

    @Override
    public List<VfsNode> ls(VfsContext ctx, String path, Integer depth) {

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
    public InputStream cat(VfsContext ctx, String path) {

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
    public String read(VfsContext ctx, String path) {

        try (InputStream is = cat(ctx, path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void write(VfsContext ctx, String path, String content) {

        write(ctx, path, content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void write(VfsContext ctx, String path, byte[] content) {

        String fullPath   = resolvePath(ctx, path);
        String parentPath = getParentPath(fullPath);

        // 确保父目录存在
        if (parentPath != null && !"/".equals(parentPath) && findByPath(ctx, parentPath).isEmpty()) {
            ensureDirs(ctx, parentPath, new HashSet<>());
        }

        Optional<SysFile> existing = findByPath(ctx, fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot write to directory: " + path);
            }
            // 更新
            try {
                storageService.delete(file.getStoragePath());
                String newStoragePath = uploadContent(content, getName(fullPath));
                file.setStoragePath(newStoragePath);
                file.setUrl(storageService.getUrl(newStoragePath));
                file.setSize((long) content.length);
                fileRepository.save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file", e);
            }
        } else {
            createFile(ctx, fullPath, content);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mv(VfsContext ctx, String srcPath, String destPath) {

        String fullSrc  = resolvePath(ctx, srcPath);
        String fullDest = resolvePath(ctx, destPath);

        SysFile src = findByPath(
                ctx,
                fullSrc
        ).orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        // 目标是已存在目录则移入
        Optional<SysFile> destOpt = findByPath(ctx, fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        // 环检测：不能把父目录移到子目录
        if (Boolean.TRUE.equals(src.getIsDirectory()) && isSubPath(fullSrc, fullDest)) {
            throw new IllegalArgumentException("Cannot move directory into itself: " + srcPath + " -> " + destPath);
        }

        // 目标已存在
        if (findByPath(ctx, fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists: " + destPath);
        }

        // 确保目标父目录存在
        String destParent = getParentPath(fullDest);
        if (destParent != null && !"/".equals(destParent) && findByPath(ctx, destParent).isEmpty()) {
            ensureDirs(ctx, destParent, new HashSet<>());
        }

        // 移动目录时更新所有子项
        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            List<SysFile> descendants = findDescendants(ctx, fullSrc + "/");
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
    public void cp(VfsContext ctx, String srcPath, String destPath) {

        String fullSrc  = resolvePath(ctx, srcPath);
        String fullDest = resolvePath(ctx, destPath);

        SysFile src = findByPath(
                ctx,
                fullSrc
        ).orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            throw new IllegalArgumentException("Directory copy not supported");
        }

        // 目标是已存在目录则复制到该目录下
        Optional<SysFile> destOpt = findByPath(ctx, fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            fullDest = fullDest + "/" + getName(fullSrc);
        }

        if (findByPath(ctx, fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists");
        }

        // 确保目标父目录存在
        String destParent = getParentPath(fullDest);
        if (destParent != null && !"/".equals(destParent) && findByPath(ctx, destParent).isEmpty()) {
            ensureDirs(ctx, destParent, new HashSet<>());
        }

        SysFile copy = new SysFile();
        copy.setBizId(IDWorker.newId());
        copy.setVirtualPath(fullDest);
        copy.setParentPath(getParentPath(fullDest));
        copy.setIsDirectory(false);
        copy.setOriginalName(getName(fullDest));
        copy.setStorageName(src.getStorageName());
        copy.setStoragePath(src.getStoragePath());
        copy.setUrl(src.getUrl());
        copy.setExtension(src.getExtension());
        copy.setContentType(src.getContentType());
        copy.setSize(src.getSize());
        copy.setStorageType(src.getStorageType());
        fileRepository.save(copy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rm(VfsContext ctx, String path) {

        String fullPath = resolvePath(ctx, path);
        SysFile file = findByPath(
                ctx,
                fullPath
        ).orElseThrow(() -> new IllegalArgumentException("Path not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            if (!findChildren(ctx, fullPath).isEmpty()) {
                throw new IllegalArgumentException("Directory not empty: " + path);
            }
        }
        fileRepository.delete(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rmrf(VfsContext ctx, String path) {

        String            fullPath = resolvePath(ctx, path);
        Optional<SysFile> opt      = findByPath(ctx, fullPath);
        if (opt.isEmpty()) return;

        SysFile file = opt.get();
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            // 删除所有子项（包括文件和子目录）
            List<SysFile> descendants = findDescendants(ctx, fullPath + "/");
            fileRepository.deleteAll(descendants);
        }
        fileRepository.delete(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mkdir(VfsContext ctx, String path) {

        String fullPath = resolvePath(ctx, path);

        if (findByPath(ctx, fullPath).isPresent()) {
            throw new IllegalArgumentException("Path already exists: " + path);
        }

        String parentPath = getParentPath(fullPath);
        if (parentPath != null && !"/".equals(parentPath) && findByPath(ctx, parentPath).isEmpty()) {
            throw new IllegalArgumentException("Parent directory not found");
        }

        createDirectory(ctx, fullPath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mkdirp(VfsContext ctx, String path) {

        String fullPath = resolvePath(ctx, path);
        if (findByPath(ctx, fullPath).isPresent()) return;
        ensureDirs(ctx, fullPath, new HashSet<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadZip(VfsContext ctx, String path, InputStream zipStream) {

        String basePath = resolvePath(ctx, path);

        // 确保目标目录存在
        if (!"/".equals(basePath) && findByPath(ctx, basePath).isEmpty()) {
            ensureDirs(ctx, basePath, new HashSet<>());
        }

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

                    byte[] content = zis.readAllBytes();
                    if (findByPath(ctx, entryPath).isEmpty()) {
                        createFile(ctx, entryPath, content);
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract zip", e);
        }

    }

    @Override
    public boolean exists(VfsContext ctx, String path) {

        return findByPath(ctx, resolvePath(ctx, path)).isPresent();
    }

    @Override
    public boolean isDir(VfsContext ctx, String path) {

        return findByPath(ctx, resolvePath(ctx, path)).map(f -> Boolean.TRUE.equals(f.getIsDirectory())).orElse(false);
    }

}
