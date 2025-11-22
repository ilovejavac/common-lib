package com.dev.lib.storage.data;

import com.dev.lib.entity.BaseRepository;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.storage.QSysFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Optional;

public interface SysFileRepository extends BaseRepository<SysFile> {
    QSysFile q = QSysFile.sysFile;

    @EqualsAndHashCode(callSuper = true)
    @Data
    class Query extends DslQuery<SysFile> {
        @Condition(type = QueryType.EQ)
        private String md5;
    }

    Optional<SysFile> findByBizId(String bizId);

    Optional<SysFile> findByMd5(String md5);
}
