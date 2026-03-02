package com.dev.lib.storage.domain.service.virtual.repository;

import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
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

    private final StorageServiceNameProvider serviceNameProvider;

    private String currentService() {

        return serviceNameProvider.currentServiceName();
    }

    // ==================== 单记录查询 ====================

    public Optional<SysFile> findByPath(String virtualPath) {

        return fileSystemRepository.findByVirtualPath(currentService(), virtualPath);
    }

    public Optional<SysFile> findByPathForUpdate(String virtualPath) {

        return fileSystemRepository.findByVirtualPathForUpdate(currentService(), virtualPath);
    }

    // ==================== 多记录查询 ====================

    public List<SysFile> findChildren(String parentPath) {

        return fileSystemRepository.findByParentPath(currentService(), parentPath);
    }

    public List<SysFile> findDescendants(String prefix) {

        return fileSystemRepository.findByVirtualPathStartingWith(currentService(), prefix);
    }

    public List<SysFile> findDescendantsForUpdate(String prefix) {

        return fileSystemRepository.findByVirtualPathStartingWithForUpdate(currentService(), prefix);
    }

    public List<SysFile> findByStoragePath(String storagePath) {

        return fileSystemRepository.findByStoragePath(currentService(), storagePath);
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
