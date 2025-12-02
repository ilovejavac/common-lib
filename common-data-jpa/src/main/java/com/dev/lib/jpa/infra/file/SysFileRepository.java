package com.dev.lib.jpa.infra.file;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SysFileRepository extends BaseRepository<SysFile> {
    QSysFile q = QSysFile.sysFile;


    @Data
    class Query extends DslQuery<SysFile> {
        @Condition(type = QueryType.EQ)
        private String md5;

        private Collection<String> bizIdIn;
    }

    default List<SysFile> findAllByBizIdIn(Collection<String> bizIds) {
        return loads(new Query().setBizIdIn(bizIds));
    }

    Optional<SysFile> findByBizId(String bizId);

    Optional<SysFile> findByMd5(String md5);

    default List<SysFile> findByIds(Collection<String> ids) {
        return loads(new Query().setBizIdIn(ids));
    }
}
