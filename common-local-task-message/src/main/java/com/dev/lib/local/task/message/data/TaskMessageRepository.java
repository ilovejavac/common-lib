package com.dev.lib.local.task.message.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskMessageRepository extends BaseRepository<LocalTaskMessagePo> {

    QLocalTaskMessagePo $ = QLocalTaskMessagePo.localTaskMessagePo;

    @Data
    class Query extends DslQuery<LocalTaskMessagePo> {

        private List<Integer> houseNumberIn;

        private Long idGe;

        private String taskId;

        private LocalTaskStatus status;

        private String taskType;

    }

    public default Optional<LocalTaskMessagePo> loadById(String id) {

        return load(new Query().setTaskId(id));
    }

    /**
     * 按 taskId 查询任务
     */
    default Optional<LocalTaskMessagePo> loadByTaskId(String taskId) {
        return load(new Query().setTaskId(taskId));
    }

    default List<LocalTaskMessagePo> loadsByHouseNumber(List<Integer> houseNumbers, Long id, Integer limit) {

        return loads(new Query().setHouseNumberIn(houseNumbers).setIdGe(id).setLimit(limit).setSortStr("id_asc"));
    }

    /**
     * 查询已到重试时间的 PENDING 任务
     */
    default List<LocalTaskMessagePo> loadsDuePendingByHouseNumber(
            List<Integer> houseNumbers,
            Long id,
            Integer limit,
            String taskType,
            LocalDateTime now
    ) {
        Query query = new Query();
        query.setHouseNumberIn(houseNumbers);
        query.setIdGe(id);
        query.setStatus(LocalTaskStatus.PENDING);
        query.setLimit(limit);
        query.setSortStr("id_asc");
        if (taskType != null) {
            query.setTaskType(taskType);
        }

        return loads(query, $.nextRetryTime.isNull().or($.nextRetryTime.loe(now)));
    }

    /**
     * 查询 PROCESSING 候选任务（用于超时恢复）
     */
    default List<LocalTaskMessagePo> loadsProcessingByHouseNumber(
            List<Integer> houseNumbers,
            Long id,
            Integer limit,
            String taskType
    ) {
        Query query = new Query();
        query.setHouseNumberIn(houseNumbers);
        query.setIdGe(id);
        query.setStatus(LocalTaskStatus.PROCESSING);
        query.setLimit(limit);
        query.setSortStr("id_asc");
        if (taskType != null) {
            query.setTaskType(taskType);
        }

        return loads(query, $.processedAt.isNotNull());
    }

}
