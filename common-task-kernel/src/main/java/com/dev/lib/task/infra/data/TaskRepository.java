package com.dev.lib.task.infra.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends BaseRepository<TaskPo> {

    QTaskPo $ = QTaskPo.taskPo;

    @Data
    class Query extends DslQuery<TaskPo> {

        private String taskId;

        private TaskMode mode;

        private TaskStatus status;

        private LocalDateTime nextRunAtLe;

    }

    default Optional<TaskPo> loadByTaskId(String taskId) {

        return load(new Query().setTaskId(taskId));
    }

    default List<TaskPo> loadsDueSchedules(LocalDateTime now, int limit) {

        Query query = new Query();
        query.setMode(TaskMode.SCHEDULE);
        query.setStatus(TaskStatus.ENABLED);
        query.setLimit(limit);
        query.setSortStr("nextRunAt_asc,id_asc");
        return loads(query, $.nextRunAt.isNotNull().and($.nextRunAt.loe(now)));
    }

    default List<TaskPo> loadsSchedules() {

        Query query = new Query();
        query.setMode(TaskMode.SCHEDULE);
        query.setSortStr("id_asc");
        return loads(query);
    }

    default BooleanExpression byTaskId(String taskId) {

        return $.taskId.eq(taskId);
    }

}
