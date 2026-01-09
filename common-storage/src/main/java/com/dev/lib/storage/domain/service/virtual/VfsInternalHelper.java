package com.dev.lib.storage.domain.service.virtual;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;

import static com.dev.lib.storage.domain.service.virtual.VfsPathUtils.*;

/**
 * VFS 内部辅助服务
 */
@Component
@RequiredArgsConstructor
public class VfsInternalHelper {

    private final SysFileRepository      fileRepository;
    private final FileSystemRepository   systemRepository;
    private final StorageService         storageService;
    private final AppStorageProperties   storageProperties;

    // ==================== 路径解析 ====================

    public String resolvePath(VfsContext ctx, String path) {
        return VfsPathUtils.resolvePath(ctx.getRoot(), path);
    }

    // ==================== 查询方法 ====================

    public Optional<SysFile> findByPath(VfsContext ctx, String virtualPath) {
        return systemRepository.findByVirtualPath(virtualPath);
    }

    public Optional<SysFile> findByPathForUpdate(VfsContext ctx, String virtualPath) {
        return systemRepository.findByVirtualPathForUpdate(virtualPath);
    }

    public List<SysFile> findChildren(VfsContext ctx, String parentPath) {
        return systemRepository.findByParentPath(parentPath);
    }

    public List<SysFile> findDescendants(VfsContext ctx, String prefix) {
        return systemRepository.findByVirtualPathStartingWith(prefix);
    }

    public List<SysFile> findDescendantsForUpdate(VfsContext ctx, String prefix) {
        return systemRepository.findByVirtualPathStartingWithForUpdate(prefix);
    }

    // ==================== 目录创建 ====================

    /**
     * 递归创建目录（使用数据库唯一约束防止并发重复创建）
     */
    public void ensureDirs(VfsContext ctx, String fullPath, Set<String> created) {

        if (fullPath == null || "/".equals(fullPath) || created.contains(fullPath)) {
            return;
        }
        if (findByPath(ctx, fullPath).isPresent()) {
            return;
        }

        String parent = getParentPath(fullPath);
        if (parent != null && !"/".equals(parent)) {
            ensureDirs(ctx, parent, created);
        }

        try {
            createDirectoryInternal(fullPath);
            created.add(fullPath);
        } catch (DataIntegrityViolationException e) {
            if (findByPath(ctx, fullPath).isPresent()) {
                created.add(fullPath);
            } else {
                throw new RuntimeException("Failed to create directory: " + fullPath, e);
            }
        }
    }

    /**
     * 创建单个目录记录
     */
    public void createDirectoryInternal(String virtualPath) {

        SysFile dir = new SysFile();
        dir.setBizId(IDWorker.newId());
        dir.setVirtualPath(virtualPath);
        dir.setParentPath(getParentPath(virtualPath));
        dir.setIsDirectory(true);
        dir.setOriginalName(getName(virtualPath));
        dir.setStorageName(getName(virtualPath));
        fileRepository.save(dir);
    }

    // ==================== 文件创建 ====================

    /**
     * 创建文件并上传内容
     */
    public String createFile(VfsContext ctx, String virtualPath, InputStream inputStream, long size) {

        String fileName  = getName(virtualPath);
        String extension = getExtension(fileName);

        try {
            String storagePath = uploadContent(inputStream, fileName);

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

    /**
     * 上传内容到存储服务
     */
    public String uploadContent(InputStream inputStream, String fileName) throws IOException {

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

    // ==================== 节点转换 ====================

    public List<VfsNode> buildNodes(VfsContext ctx, List<SysFile> files, int depth) {

        List<VfsNode> nodes = new ArrayList<>();
        for (SysFile file : files) {
            nodes.add(toNode(ctx, file, depth - 1));
        }
        return nodes;
    }

    public VfsNode toNode(VfsContext ctx, SysFile file, int remainingDepth) {

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

    // ==================== 其他辅助方法 ====================

    public SysFileRepository getFileRepository() {
        return fileRepository;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public AppStorageProperties getStorageProperties() {
        return storageProperties;
    }
}
