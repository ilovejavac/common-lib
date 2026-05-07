package com.dev.lib.task.infra.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.task.domain.TaskRunStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRunRepository extends BaseRepository<TaskRunPo> {

    QTaskRunPo $ = QTaskRunPo.taskRunPo;

    @Data
    class Query extends DslQuery<TaskRunPo> {

        private String runId;

        private String taskId;

        private TaskRunStatus status;

        private LocalDateTime scheduledAtLe;

        private LocalDateTime nextRetryAtLe;

        private LocalDateTime leaseExpireAtLe;

    }

    default Optional<TaskRunPo> loadByRunId(String runId) {

        return load(new Query().setRunId(runId));
    }

    default List<TaskRunPo> loadsByTaskId(String taskId) {

        Query query = new Query();
        query.setTaskId(taskId);
        query.setSortStr("id_desc");
        return loads(query);
    }

    default Optional<TaskRunPo> loadLatestByTaskId(String taskId) {

        Query query = new Query();
        query.setTaskId(taskId);
        query.setSortStr("id_desc");
        query.setLimit(1);
        return loads(query).stream().findFirst();
    }

    default List<TaskRunPo> loadsRunnableForUpdate(LocalDateTime now, int limit) {

        Query query = new Query();
        query.setStatus(TaskRunStatus.PENDING);
        query.setLimit(limit);
        query.setSortStr("scheduledAt_asc,id_asc");
        BooleanExpression dueNow = $.scheduledAt.isNotNull().and($.scheduledAt.loe(now));
        BooleanExpression retryDue = $.nextRetryAt.isNotNull().and($.nextRetryAt.loe(now));
        return lockForUpdate().skipLocked().loads(query, dueNow.or(retryDue));
    }

    default List<TaskRunPo> loadsExpiredRunning(LocalDateTime now, int limit) {

        Query query = new Query();
        query.setStatus(TaskRunStatus.RUNNING);
        query.setLimit(limit);
        query.setSortStr("leaseExpireAt_asc,id_asc");
        return loads(query, $.leaseExpireAt.isNotNull().and($.leaseExpireAt.loe(now)));
    }

    default boolean existsByTaskIdAndTriggerTime(String taskId, LocalDateTime triggerTime) {

        return exists($.taskId.eq(taskId), $.triggerTime.eq(triggerTime));
    }

}
