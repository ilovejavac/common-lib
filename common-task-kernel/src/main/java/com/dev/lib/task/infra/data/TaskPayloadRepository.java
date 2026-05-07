package com.dev.lib.task.infra.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.util.Optional;

public interface TaskPayloadRepository extends BaseRepository<TaskPayloadPo> {

    @Data
    class Query extends DslQuery<TaskPayloadPo> {

        private String payloadId;

        private String taskId;

    }

    default Optional<TaskPayloadPo> loadByPayloadId(String payloadId) {

        return load(new Query().setPayloadId(payloadId));
    }

}
