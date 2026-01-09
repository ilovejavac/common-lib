package com.dev.lib.storage.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SysFileRepository extends BaseRepository<SysFile> {

    QSysFile q = QSysFile.sysFile;

    @Data
    class Query extends DslQuery<SysFile> {

        private Boolean temporary;

        private LocalDateTime deleteAfterLe;

        private Collection<String> bizIdIn;

    }

    default List<SysFile> findAllByBizIdIn(Collection<String> bizIds) {

        return loads(new Query().setBizIdIn(bizIds));
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

    Optional<SysFile> findByBizId(String bizId);

    default List<SysFile> findByIds(Collection<String> ids) {

        return loads(new Query().setBizIdIn(ids));
    }

}
