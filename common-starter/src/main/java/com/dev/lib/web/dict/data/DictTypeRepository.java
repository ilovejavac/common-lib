package com.dev.lib.web.dict.data;

import com.dev.lib.entity.BaseRepository;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.DslQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends BaseRepository<DictType> {
    QDictType q = QDictType.dictType;

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Accessors(chain = true)
    class Query extends DslQuery<DictType> {

    }

    default Optional<DictType> getType(String id) {
        Query query = new Query();
        query.id = id;

        return load(query, q.status.eq(EntityStatus.ENABLE));
    }

}