package com.dev.lib.jpa.infra.localTaskMessage;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

public interface TaskMessageRepository extends BaseRepository<LocalTaskMessageEntity> {
    QLocalTaskMessageEntity $ = QLocalTaskMessageEntity.localTaskMessageEntity;

    @Data
    class Query extends DslQuery<LocalTaskMessageEntity> {

    }


}
