package com.dev.lib.storage.domain.service.virtual.core;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.virtual.VfsPathUtils;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * VFS 目录服务 - 扁平化核心服务
 * <p>
 * 职责：
 * - 目录创建（mkdir, mkdirp）
 * - 目录列表（ls, tree）
 * - 文件移动/复制/删除（mv, cp, rm）
 * - 通配符展开（expandWildcard）
 * <p>
 * 设计原则：
 * - 迭代替代递归创建目录（避免栈溢出）
 * - 嵌套深度控制在 3-4 层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VfsCoreDirectoryService {

    private final VfsFileRepository fileRepository;
    private final VfsPathResolver pathResolver;
    private final StorageServiceNameProvider serviceNameProvider;
    private final VfsFileStorageService storageService;
    private final VfsFileService fileService;

    // ========== 目录创建 ==========

    /**
     * 创建目录（迭代实现，自动创建父目录）
     */
    @Transactional
    public void mkdirp(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        if (fileRepository.findByPath(fullPath).isPresent()) {
            return;
        }

        // 收集需要创建的所有目录路径（从根到目标）
        List<String> pathsToCreate = new ArrayList<>();
        String current = fullPath;

        while (current != null && !"/".equals(current)) {
            if (fileRepository.findByPath(current).isEmpty()) {
                pathsToCreate.add(0, current);
            }
            current = pathResolver.getParent(current);
        }

        // 按顺序创建目录
        for (String path : pathsToCreate) {
            createSingleDirectory(ctx, path);
        }
    }

    /**
     * 创建单个目录（不创建父目录）
     */
    @Transactional
    public void mkdir(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        String parentPath = pathResolver.getParent(fullPath);
        if (parentPath != null && !"/".equals(parentPath) && fileRepository.findByPath(parentPath).isEmpty()) {
            throw new IllegalArgumentException("Parent directory not found: " + parentPath);
        }

        if (fileRepository.findByPath(fullPath).isPresent()) {
            throw new IllegalArgumentException("Path already exists: " + virtualPath);
        }

        createSingleDirectory(ctx, fullPath);
    }

    // ========== 目录列表 ==========

    /**
     * 列出目录内容（支持通配符）
     */
    @Transactional(readOnly = true)
    public List<VfsNode> list(VfsContext ctx, String virtualPath, boolean showHidden) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        // 处理通配符
        if (fullPath.contains("*") || fullPath.contains("?")) {
            return expandWildcard(fullPath, showHidden);
        }

        // 根目录
        if ("/".equals(fullPath) || fullPath.isEmpty()) {
            return fileRepository.findChildren("/").stream()
                .filter(file -> showHidden || !Boolean.TRUE.equals(file.getHidden()))
                .map(this::toVfsNode)
                .collect(Collectors.toList());
        }

        SysFile dir = fileRepository.findByPath(fullPath)
            .orElseThrow(() -> new IllegalArgumentException("Directory not found: " + virtualPath));

        if (!Boolean.TRUE.equals(dir.getIsDirectory())) {
            return List.of(toVfsNode(dir));
        }

        return fileRepository.findChildren(fullPath).stream()
            .filter(file -> showHidden || !Boolean.TRUE.equals(file.getHidden()))
            .map(this::toVfsNode)
            .collect(Collectors.toList());
    }

    /**
     * 树形显示目录结构
     */
    @Transactional(readOnly = true)
    public String tree(VfsContext ctx, String virtualPath, int maxDepth) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);
        StringBuilder sb = new StringBuilder();
        sb.append(pathResolver.getName(fullPath)).append("\n");
        buildTree(fullPath, "", 0, maxDepth, sb);
        return sb.toString();
    }

    // ========== 文件系统操作 ==========

    /**
     * 移动文件/目录
     */
    @Transactional
    public void move(VfsContext ctx, String srcPath, String destPath) {
        String srcFullPath = pathResolver.resolve(ctx, srcPath);
        String destFullPath = pathResolver.resolve(ctx, destPath);

        SysFile srcFile = fileRepository.findByPathForUpdate(srcFullPath)
            .orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        if (fileRepository.findByPath(destFullPath).isPresent()) {
            throw new IllegalArgumentException("Destination already exists: " + destPath);
        }

        // 确保目标父目录存在
        String destParent = pathResolver.getParent(destFullPath);
        if (destParent != null && !"/".equals(destParent)) {
            mkdirp(ctx, destParent);
        }

        // 更新路径
        srcFile.setVirtualPath(destFullPath);
        srcFile.setParentPath(destParent);
        srcFile.setOriginalName(pathResolver.getName(destFullPath));
        fileRepository.save(srcFile);

        // 如果是目录，递归更新子路径
        if (Boolean.TRUE.equals(srcFile.getIsDirectory())) {
            updateChildrenPaths(srcFullPath, destFullPath);
        }
    }

    /**
     * 复制文件/目录
     */
    @Transactional
    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {
        String srcFullPath = pathResolver.resolve(ctx, srcPath);
        String destFullPath = pathResolver.resolve(ctx, destPath);

        SysFile srcFile = fileRepository.findByPath(srcFullPath)
            .orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        if (Boolean.TRUE.equals(srcFile.getIsDirectory())) {
            if (!recursive) {
                throw new IllegalArgumentException("Cannot copy directory without recursive flag");
            }
            copyDirectory(ctx, srcFullPath, destFullPath);
        } else {
            copyFile(ctx, srcFile, destFullPath);
        }
    }

    /**
     * 删除文件/目录
     */
    @Transactional
    public void delete(VfsContext ctx, String virtualPath, boolean recursive) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        SysFile file = fileRepository.findByPath(fullPath)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + virtualPath));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            List<SysFile> children = fileRepository.findChildren(fullPath);
            if (!children.isEmpty() && !recursive) {
                throw new IllegalArgumentException("Directory not empty: " + virtualPath);
            }

            // 递归删除子文件和子目录
            for (SysFile child : children) {
                delete(ctx, child.getVirtualPath(), true);
            }

            // 删除目录记录
            fileRepository.delete(file);
        } else {
            // 删除文件
            fileService.delete(ctx, file.getVirtualPath());
        }
    }

    /**
     * 检查是否是目录
     */
    @Transactional(readOnly = true)
    public boolean isDirectory(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);
        return fileRepository.findByPath(fullPath)
            .map(file -> Boolean.TRUE.equals(file.getIsDirectory()))
            .orElse(false);
    }

    // ========== 通配符支持 ==========

    /**
     * 展开通配符路径，返回匹配的 VfsNode 列表
     */
    @Transactional(readOnly = true)
    public List<VfsNode> expandWildcard(String fullPattern, boolean showHidden) {
        int lastSlash = fullPattern.lastIndexOf('/');
        String dir = lastSlash > 0 ? fullPattern.substring(0, lastSlash) : "/";
        String filePattern = fullPattern.substring(lastSlash + 1);

        if (fileRepository.findByPath(dir).isEmpty() && !"/".equals(dir)) {
            return List.of();
        }

        List<SysFile> children = fileRepository.findChildren(dir);

        return children.stream()
            .filter(file -> showHidden || !Boolean.TRUE.equals(file.getHidden()))
            .filter(file -> VfsPathUtils.matchPattern(pathResolver.getName(file.getVirtualPath()), filePattern))
            .map(this::toVfsNode)
            .collect(Collectors.toList());
    }

    // ========== 搜索 ==========

    /**
     * 按名称模式搜索（支持通配符）
     */
    @Transactional(readOnly = true)
    public List<VfsNode> findByPattern(VfsContext ctx, String basePath, String pattern) {
        String fullBasePath = pathResolver.resolve(ctx, basePath);
        List<SysFile> descendants = fileRepository.findDescendants(fullBasePath);

        return descendants.stream()
            .filter(file -> VfsPathUtils.matchPattern(pathResolver.getName(file.getVirtualPath()), pattern))
            .map(this::toVfsNode)
            .collect(Collectors.toList());
    }

    /**
     * 按内容搜索（流式读取，不加载到内存）
     */
    @Transactional(readOnly = true)
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content) {
        String fullBasePath = pathResolver.resolve(ctx, basePath);
        List<SysFile> descendants = fileRepository.findDescendants(fullBasePath);
        List<VfsNode> results = new ArrayList<>();

        for (SysFile file : descendants) {
            if (Boolean.TRUE.equals(file.getIsDirectory())) continue;

            try (InputStream input = storageService.download(file.getStoragePath());
                 var reader = new java.io.BufferedReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(content)) {
                        results.add(toVfsNode(file));
                        break;
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to search content in file: {}", file.getVirtualPath(), e);
            }
        }

        return results;
    }

    // ========== 私有辅助方法 ==========

    private void createSingleDirectory(VfsContext ctx, String fullPath) {
        try {
            String name = pathResolver.getName(fullPath);
            SysFile dir = new SysFile();
            dir.setBizId(IDWorker.newId());
            dir.setVirtualPath(fullPath);
            dir.setParentPath(pathResolver.getParent(fullPath));
            dir.setIsDirectory(true);
            dir.setOriginalName(name);
            dir.setStorageName(name);
            dir.setHidden(name.startsWith("."));
            dir.setServiceName(serviceNameProvider.resolve(ctx));
            fileRepository.save(dir);
        } catch (DataIntegrityViolationException e) {
            if (fileRepository.findByPath(fullPath).isEmpty()) {
                throw new RuntimeException("Failed to create directory: " + fullPath, e);
            }
            log.debug("Directory already exists (concurrent creation): {}", fullPath);
        }
    }

    private void buildTree(String path, String prefix, int depth, int maxDepth, StringBuilder sb) {
        if (depth >= maxDepth) return;

        List<SysFile> children = fileRepository.findChildren(path);
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = (i == children.size() - 1);
            String connector = isLast ? "└── " : "├── ";
            String childPrefix = isLast ? "    " : "│   ";
            SysFile child = children.get(i);
            String name = pathResolver.getName(child.getVirtualPath());

            sb.append(prefix).append(connector).append(name).append("\n");

            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                buildTree(child.getVirtualPath(), prefix + childPrefix, depth + 1, maxDepth, sb);
            }
        }
    }

    private void updateChildrenPaths(String oldParent, String newParent) {
        List<SysFile> children = fileRepository.findChildren(oldParent);
        for (SysFile child : children) {
            String oldPath = child.getVirtualPath();
            String newPath = newParent + oldPath.substring(oldParent.length());
            child.setVirtualPath(newPath);
            child.setParentPath(newParent);
            fileRepository.save(child);

            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                updateChildrenPaths(oldPath, newPath);
            }
        }
    }

    private void copyFile(VfsContext ctx, SysFile srcFile, String destFullPath) {
        String destParent = pathResolver.getParent(destFullPath);
        if (destParent != null && !"/".equals(destParent)) {
            mkdirp(ctx, destParent);
        }

        // 复制存储层文件
        String newStoragePath = null;
        if (srcFile.getStoragePath() != null) {
            try {
                newStoragePath = storageService.generateStoragePath(pathResolver.getName(destFullPath), null);
                storageService.copy(srcFile.getStoragePath(), newStoragePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy file content: " + srcFile.getVirtualPath(), e);
            }
        }

        // 创建新的文件记录
        SysFile copy = new SysFile();
        copy.setBizId(IDWorker.newId());
        copy.setVirtualPath(destFullPath);
        copy.setParentPath(destParent);
        copy.setIsDirectory(false);
        copy.setOriginalName(pathResolver.getName(destFullPath));
        copy.setStorageName(srcFile.getStorageName());
        copy.setStoragePath(newStoragePath);
        copy.setExtension(srcFile.getExtension());
        copy.setContentType(srcFile.getContentType());
        copy.setSize(srcFile.getSize());
        copy.setStorageType(srcFile.getStorageType());
        copy.setServiceName(serviceNameProvider.resolve(ctx));
        fileRepository.save(copy);
    }

    private void copyDirectory(VfsContext ctx, String srcFullPath, String destFullPath) {
        mkdirp(ctx, destFullPath);

        List<SysFile> children = fileRepository.findChildren(srcFullPath);
        for (SysFile child : children) {
            String childName = pathResolver.getName(child.getVirtualPath());
            String destChildPath = destFullPath + "/" + childName;

            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                copyDirectory(ctx, child.getVirtualPath(), destChildPath);
            } else {
                copyFile(ctx, child, destChildPath);
            }
        }
    }

    private VfsNode toVfsNode(SysFile file) {
        VfsNode node = new VfsNode();
        node.setId(file.getBizId());
        node.setName(pathResolver.getName(file.getVirtualPath()));
        node.setPath(file.getVirtualPath());
        node.setIsDirectory(file.getIsDirectory());
        node.setSize(file.getSize());
        node.setExtension(file.getExtension());
        node.setModifiedAt(file.getUpdatedAt());
        return node;
    }
}
