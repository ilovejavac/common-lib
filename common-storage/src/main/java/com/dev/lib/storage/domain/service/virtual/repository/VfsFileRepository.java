package com.dev.lib.storage.domain.service.virtual.repository;

import com.dev.lib.storage.data.VfsPathRepository;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * VFS 文件领域仓储
 * 封装 VfsPathRepository，提供领域特定的查询方法
 */
@Component
@RequiredArgsConstructor
public class VfsFileRepository {

    private final VfsPathRepository vfsPathRepository;

    private final StorageServiceNameProvider serviceNameProvider;

    private String currentService() {

        return serviceNameProvider.currentServiceName();
    }

    // ==================== 单记录查询 ====================

    public Optional<SysFile> findByPath(String virtualPath) {

        return vfsPathRepository.findByVirtualPath(currentService(), virtualPath);
    }

    public Optional<SysFile> findByPathForUpdate(String virtualPath) {

        return vfsPathRepository.findByVirtualPathForUpdate(currentService(), virtualPath);
    }

    // ==================== 多记录查询 ====================

    public List<SysFile> findChildren(String parentPath) {

        return vfsPathRepository.findByParentPath(currentService(), parentPath);
    }

    public List<SysFile> findDescendants(String prefix) {

        return vfsPathRepository.findByVirtualPathStartingWith(currentService(), prefix);
    }

    public List<SysFile> findDescendantsForUpdate(String prefix) {

        return vfsPathRepository.findByVirtualPathStartingWithForUpdate(currentService(), prefix);
    }

    public List<SysFile> findByStoragePath(String storagePath) {

        return vfsPathRepository.findByStoragePath(currentService(), storagePath);
    }

    // ==================== 保存和删除 ====================

    public void save(SysFile file) {

        vfsPathRepository.save(file);
    }

    public void saveAll(List<SysFile> files) {

        vfsPathRepository.saveAll(files);
    }

    public void delete(SysFile file) {

        vfsPathRepository.delete(file);
    }

    public void deleteAll(List<SysFile> files) {

        vfsPathRepository.deleteAll(files);
    }

}
