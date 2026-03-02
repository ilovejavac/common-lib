package com.dev.lib.storage.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileSystemRepository extends BaseRepository<SysFile> {

    @Data
    class Query extends DslQuery<SysFile> {

        private String serviceName;

        private String virtualPath;       // 逻辑路径: "/a/d/d3.md"

        private String parentPath;        // 父路径: "/a/d" (加速查询)

        private String virtualPathStartWith;

        private Collection<String> virtualPathIn;

        private String storagePath;       // 存储路径（用于 COW 检查）

    }

    default Optional<SysFile> findByVirtualPath(String serviceName, String virtualPath) {

        return load(new Query().setServiceName(serviceName).setVirtualPath(virtualPath));
    }

    /**
     * 悲观锁查询，用于并发写操作
     */
    default Optional<SysFile> findByVirtualPathForUpdate(String serviceName, String virtualPath) {

        return lockForUpdate().load(new Query().setServiceName(serviceName).setVirtualPath(virtualPath));
    }

    default List<SysFile> findByParentPath(String serviceName, String parentPath) {

        return loads(new Query().setServiceName(serviceName).setParentPath(parentPath));
    }

    default List<SysFile> findByVirtualPathStartingWith(String serviceName, String prefix) {

        return loads(new Query().setServiceName(serviceName).setVirtualPathStartWith(prefix));
    }

    /**
     * 批量悲观锁查询
     */
    default List<SysFile> findByVirtualPathsForUpdate(String serviceName, List<String> paths) {

        return lockForUpdate().loads(new Query().setServiceName(serviceName).setVirtualPathIn(paths));
    }

    /**
     * 按前缀悲观锁查询（用于移动目录时锁定所有子项）
     */
    default List<SysFile> findByVirtualPathStartingWithForUpdate(String serviceName, String prefix) {

        return lockForUpdate().loads(new Query().setServiceName(serviceName).setVirtualPathStartWith(prefix));
    }

    /**
     * 按 storagePath 查询（用于 COW 检查）
     */
    default List<SysFile> findByStoragePath(String serviceName, String storagePath) {

        return loads(new Query().setServiceName(serviceName).setStoragePath(storagePath));
    }

}
