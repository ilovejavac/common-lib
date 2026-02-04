package com.dev.lib.local.task.message.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface TaskMessageRepository extends BaseRepository<LocalTaskMessagePo> {

    // QLocalTaskMessagePo $ = QLocalTaskMessagePo.localTaskMessagePo;

    class Query extends DslQuery<LocalTaskMessagePo> {

        private List<Integer> houseNumberIn;

        private Long idGe;

        private String taskId;

        // 手动添加 setter 方法
        public Query setHouseNumberIn(List<Integer> houseNumberIn) {
            this.houseNumberIn = houseNumberIn;
            return this;
        }

        public Query setIdGe(Long idGe) {
            this.idGe = idGe;
            return this;
        }

        public Query setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

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

}
