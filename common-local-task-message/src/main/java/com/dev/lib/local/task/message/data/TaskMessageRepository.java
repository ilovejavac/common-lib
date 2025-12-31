package com.dev.lib.local.task.message.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.util.List;
import java.util.Optional;

public interface TaskMessageRepository extends BaseRepository<LocalTaskMessagePo> {

    QLocalTaskMessagePo $ = QLocalTaskMessagePo.localTaskMessagePo;

    @Data
    class Query extends DslQuery<LocalTaskMessagePo> {

        private List<Integer> houseNumberIn;

        private Long          idGe;

    }

    default Optional<LocalTaskMessagePo> loadById(String id) {

        return load(new Query().setBizId(id));
    }

    default List<LocalTaskMessagePo> loadsByHouseNumber(List<Integer> houseNumbers, Long id, Integer limit) {

        return loads(new Query().setHouseNumberIn(houseNumbers).setIdGe(id).setLimit(limit).setSortStr("id_asc"));
    }

}
