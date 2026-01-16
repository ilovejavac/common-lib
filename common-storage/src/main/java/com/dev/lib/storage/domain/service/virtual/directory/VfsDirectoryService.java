package com.dev.lib.storage.domain.service.virtual.directory;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * VFS 目录服务
 * 负责目录的创建和确保操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsDirectoryService {

    private final VfsFileRepository fileRepository;
    private final VfsPathResolver   pathResolver;

    /**
     * 递归创建目录（使用数据库唯一约束防止并发重复创建）
     *
     * @param ctx      VFS 上下文
     * @param fullPath 完整路径
     * @param created  已创建的目录集合（用于递归）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureDirs(VfsContext ctx, String fullPath, Set<String> created) {

        if (fullPath == null || "/".equals(fullPath) || created.contains(fullPath)) {
            return;
        }
        if (fileRepository.findByPath(fullPath).isPresent()) {
            return;
        }

        String parent = pathResolver.getParent(fullPath);
        if (parent != null && !"/".equals(parent)) {
            ensureDirs(ctx, parent, created);
        }

        createDirectoryInternal(fullPath);
        created.add(fullPath);
    }

    /**
     * 创建单个目录记录
     * 注意：此方法不使用独立事务，由调用者的事务管理
     * 这样可以避免在递归创建目录时产生过多的事务
     *
     * @param virtualPath 虚拟路径
     */
    private void createDirectoryInternal(String virtualPath) {

        try {
            String  name = pathResolver.getName(virtualPath);
            SysFile dir  = new SysFile();
            dir.setBizId(IDWorker.newId());
            dir.setVirtualPath(virtualPath);
            dir.setParentPath(pathResolver.getParent(virtualPath));
            dir.setIsDirectory(true);
            dir.setOriginalName(name);
            dir.setStorageName(name);
            dir.setHidden(name.startsWith("."));
            fileRepository.save(dir);
        } catch (DataIntegrityViolationException e) {
            // 并发创建时可能已由其他线程创建，再次检查确认
            if (fileRepository.findByPath(virtualPath).isEmpty()) {
                throw new RuntimeException("Failed to create directory: " + virtualPath, e);
            }
            log.debug("Directory already exists (concurrent creation): {}", virtualPath);
        }
    }

    /**
     * 创建单个目录（公共方法，供外部调用）
     * 使用独立事务以确保原子性
     *
     * @param virtualPath 虚拟路径
     */
    @Transactional(rollbackFor = Exception.class)
    public void createDirectory(String virtualPath) {

        if (fileRepository.findByPath(virtualPath).isPresent()) {
            return;
        }

        try {
            String  name = pathResolver.getName(virtualPath);
            SysFile dir  = new SysFile();
            dir.setBizId(IDWorker.newId());
            dir.setVirtualPath(virtualPath);
            dir.setParentPath(pathResolver.getParent(virtualPath));
            dir.setIsDirectory(true);
            dir.setOriginalName(name);
            dir.setStorageName(name);
            dir.setHidden(name.startsWith("."));
            fileRepository.save(dir);
        } catch (DataIntegrityViolationException e) {
            // 并发创建时可能已由其他线程创建
            if (fileRepository.findByPath(virtualPath).isEmpty()) {
                throw new RuntimeException("Failed to create directory: " + virtualPath, e);
            }
            log.debug("Directory already exists (concurrent creation): {}", virtualPath);
        }
    }
}
