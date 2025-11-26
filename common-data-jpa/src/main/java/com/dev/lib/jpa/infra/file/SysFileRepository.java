package com.dev.lib.jpa.infra.file;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import lombok.Data;

import java.util.Optional;

public interface SysFileRepository extends BaseRepository<SysFile> {
    QSysFile q = QSysFile.sysFile;


    @Data
    class Query extends DslQuery<SysFile> {
        @Condition(type = QueryType.EQ)
        private String md5;
    }

    Optional<SysFile> findByBizId(String bizId);

    Optional<SysFile> findByMd5(String md5);
}
