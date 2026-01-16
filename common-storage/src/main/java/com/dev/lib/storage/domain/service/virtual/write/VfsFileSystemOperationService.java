package com.dev.lib.storage.domain.service.virtual.write;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.directory.VfsDirectoryService;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * VFS 文件系统操作服务
 * 负责文件的移动、复制和删除操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsFileSystemOperationService {

    private final VfsFileRepository     fileRepository;

    private final VfsPathResolver       pathResolver;

    private final VfsFileStorageService storageService;

    private final VfsDirectoryService   directoryService;

    private final VfsVersionManager     versionManager;

    // ==================== 移动操作 ====================

    /**
     * 移动文件或目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void move(VfsContext ctx, String srcPath, String destPath) {

        String fullSrc  = pathResolver.resolve(ctx, srcPath);
        String fullDest = pathResolver.resolve(ctx, destPath);

        SysFile src = fileRepository.findByPathForUpdate(fullSrc)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        fullDest = resolveDestinationPath(fullDest, fullSrc);

        validateMoveDestination(src, fullSrc, fullDest);

        String destParent = pathResolver.getParent(fullDest);
        ensureParentExists(ctx, destParent);

        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            moveDirectory(src, fullSrc, fullDest);
        } else {
            moveFile(src, fullDest);
        }
    }

    // ==================== 复制操作 ====================

    /**
     * 复制文件或目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {

        String fullSrc  = pathResolver.resolve(ctx, srcPath);
        String fullDest = pathResolver.resolve(ctx, destPath);

        SysFile src = fileRepository.findByPath(fullSrc)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + srcPath));

        if (Boolean.TRUE.equals(src.getIsDirectory())) {
            if (!recursive) {
                throw new IllegalArgumentException(
                        "Cannot copy directory without recursive flag. Use copy(ctx, src, dest, true)");
            }
            versionManager.copyDirectoryRecursive(ctx, fullSrc, fullDest);
            return;
        }

        fullDest = resolveDestinationPath(fullDest, fullSrc);

        if (fileRepository.findByPathForUpdate(fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists");
        }

        String destParent = pathResolver.getParent(fullDest);
        if (destParent != null && !"/".equals(destParent) && fileRepository.findByPath(destParent).isEmpty()) {
            directoryService.ensureDirs(ctx, destParent, new HashSet<>());
        }

        versionManager.copyFileRecord(src, fullDest);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除文件或目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(VfsContext ctx, String path, boolean recursive) {

        String            fullPath = pathResolver.resolve(ctx, path);
        Optional<SysFile> opt      = fileRepository.findByPathForUpdate(fullPath);

        if (opt.isEmpty()) {
            if (recursive) return;
            throw new IllegalArgumentException("Path not found: " + path);
        }

        SysFile      file                 = opt.get();
        List<String> storagePathsToDelete = new ArrayList<>();

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            List<SysFile> children = fileRepository.findChildren(fullPath);
            if (!children.isEmpty() && !recursive) {
                throw new IllegalArgumentException("Directory not empty (use recursive=true to force delete): " + path);
            }
            if (recursive) {
                List<SysFile> descendants = fileRepository.findDescendants(fullPath + "/");
                for (SysFile desc : descendants) {
                    storagePathsToDelete.addAll(storageService.collectStoragePaths(desc));
                }
                fileRepository.deleteAll(descendants);
            }
        } else {
            storagePathsToDelete.addAll(storageService.collectStoragePaths(file));
        }

        fileRepository.delete(file);

        if (!storagePathsToDelete.isEmpty()) {
            storageService.deleteAll(storagePathsToDelete);
        }
    }

    // ==================== 私有辅助方法 ====================

    private void ensureParentExists(VfsContext ctx, String parentPath) {

        if (parentPath != null && !"/".equals(parentPath) && fileRepository.findByPath(parentPath).isEmpty()) {
            directoryService.ensureDirs(ctx, parentPath, new HashSet<>());
        }
    }

    private String resolveDestinationPath(String fullDest, String fullSrc) {

        Optional<SysFile> destOpt = fileRepository.findByPathForUpdate(fullDest);
        if (destOpt.isPresent() && Boolean.TRUE.equals(destOpt.get().getIsDirectory())) {
            return fullDest + "/" + pathResolver.getName(fullSrc);
        }
        return fullDest;
    }

    private void validateMoveDestination(SysFile src, String fullSrc, String fullDest) {

        if (fileRepository.findByPathForUpdate(fullDest).isPresent()) {
            throw new IllegalArgumentException("Destination already exists: " + fullDest);
        }

        if (Boolean.TRUE.equals(src.getIsDirectory()) && pathResolver.isSubPath(fullSrc, fullDest)) {
            throw new IllegalArgumentException("Cannot move directory into itself: " + fullSrc + " -> " + fullDest);
        }
    }

    private void moveDirectory(SysFile src, String fullSrc, String fullDest) {

        List<SysFile> descendants = fileRepository.findDescendantsForUpdate(fullSrc + "/");
        for (SysFile desc : descendants) {
            String newPath = fullDest + desc.getVirtualPath().substring(fullSrc.length());
            desc.setVirtualPath(newPath);
            desc.setParentPath(pathResolver.getParent(newPath));
        }
        fileRepository.saveAll(descendants);

        moveFile(src, fullDest);
    }

    private void moveFile(SysFile file, String fullPath) {

        file.setVirtualPath(fullPath);
        file.setParentPath(pathResolver.getParent(fullPath));
        file.setOriginalName(pathResolver.getName(fullPath));
        fileRepository.save(file);
    }

}
