package com.dev.lib.storage.domain.service.virtual.repository;

import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.data.SysFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * VFS 文件领域仓储
 * 封装 FileSystemRepository，提供领域特定的查询方法
 */
@Component
@RequiredArgsConstructor
public class VfsFileRepository {

    private final FileSystemRepository fileSystemRepository;

    // ==================== 单记录查询 ====================

    public Optional<SysFile> findByPath(String virtualPath) {

        return fileSystemRepository.findByVirtualPath(virtualPath);
    }

    public Optional<SysFile> findByPathForUpdate(String virtualPath) {

        return fileSystemRepository.findByVirtualPathForUpdate(virtualPath);
    }

    // ==================== 多记录查询 ====================

    public List<SysFile> findChildren(String parentPath) {

        return fileSystemRepository.findByParentPath(parentPath);
    }

    public List<SysFile> findDescendants(String prefix) {

        return fileSystemRepository.findByVirtualPathStartingWith(prefix);
    }

    public List<SysFile> findDescendantsForUpdate(String prefix) {

        return fileSystemRepository.findByVirtualPathStartingWithForUpdate(prefix);
    }

    public List<SysFile> findByStoragePath(String storagePath) {

        return fileSystemRepository.findByStoragePath(storagePath);
    }

    // ==================== 保存和删除 ====================

    public void save(SysFile file) {

        fileSystemRepository.save(file);
    }

    public void saveAll(List<SysFile> files) {

        fileSystemRepository.saveAll(files);
    }

    public void delete(SysFile file) {

        fileSystemRepository.delete(file);
    }

    public void deleteAll(List<SysFile> files) {

        fileSystemRepository.deleteAll(files);
    }

}
