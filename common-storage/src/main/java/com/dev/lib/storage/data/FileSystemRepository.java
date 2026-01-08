package com.dev.lib.storage.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileSystemRepository extends BaseRepository<SysFile> {

    @Data
    class Query extends DslQuery<SysFile> {

        private String virtualPath;       // 逻辑路径: "/a/d/d3.md"

        private String parentPath;        // 父路径: "/a/d" (加速查询)

        private String virtualPathStartWith;

        private Collection<String> virtualPathIn;

    }

    default Optional<SysFile> findByVirtualPath(String virtualPath) {

        return load(new Query().setVirtualPath(virtualPath));
    }

    /**
     * 悲观锁查询，用于并发写操作
     */
    default Optional<SysFile> findByVirtualPathForUpdate(@Param("virtualPath") String virtualPath) {

        return lockForUpdate().load(new Query().setVirtualPath(virtualPath));
    }

    default List<SysFile> findByParentPath(String parentPath) {

        return loads(new Query().setParentPath(parentPath));
    }

    default List<SysFile> findByVirtualPathStartingWith(String prefix) {

        return loads(new Query().setVirtualPathStartWith(prefix));
    }

    /**
     * 批量悲观锁查询
     */
    default List<SysFile> findByVirtualPathsForUpdate(@Param("paths") List<String> paths) {

        return lockForUpdate().loads(new Query().setVirtualPathIn(paths));
    }

}
