package com.dev.lib.storage.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * SysFile 业务 ID 查询 Repository
 * 基于 bizId（业务 ID）查询文件
 *
 * 使用场景：
 * - 适配器层：通过 bizId 定位文件
 * - 批量操作：通过 bizIdIn 批量查询/删除
 *
 * 查询维度：
 * - bizId：业务 ID（主键）
 * - serviceName：服务归属
 */
public interface SysFileBizIdRepository extends BaseRepository<SysFile> {

    QSysFile q = QSysFile.sysFile;

    @Data
    class Query extends DslQuery<SysFile> {

        private String bizId;

        private String serviceName;

    }

    default List<SysFile> findAllByBizIdIn(Collection<String> bizIds) {

        return loads(new Query().setBizIdIn(bizIds));
    }

    default Optional<SysFile> findByBizIdAndServiceName(String bizId, String serviceName) {

        return load(new Query().setBizId(bizId).setServiceName(serviceName));
    }

    default List<SysFile> findAllByBizIdInAndServiceName(Collection<String> bizIds, String serviceName) {

        return loads(new Query().setServiceName(serviceName).setBizIdIn(bizIds));
    }

    default Optional<SysFile> findByBizIdForUpdate(String bizId) {

        return lockForUpdate().load(new Query().setBizId(bizId));
    }

    /**
     * 批量删除文件记录
     */
    default void deleteAllByBizIdIn(Collection<String> bizIds) {

        if (bizIds == null || bizIds.isEmpty()) {
            return;
        }
        // 使用 QueryDSL 批量删除
        delete(new Query().setBizIdIn(bizIds));
    }

    default void deleteAllByBizIdInAndServiceName(Collection<String> bizIds, String serviceName) {

        if (bizIds == null || bizIds.isEmpty()) {
            return;
        }
        delete(new Query().setServiceName(serviceName).setBizIdIn(bizIds));
    }

    Optional<SysFile> findByBizId(String bizId);

    default List<SysFile> findByIds(Collection<String> ids) {

        return loads(new Query().setBizIdIn(ids));
    }

}
