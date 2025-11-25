package com.dev.lib.jpa.data;

import com.dev.lib.dict.model.dto.DictTypeDTO;
import com.dev.lib.jpa.entity.JpaDsl;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.web.model.QueryRequest;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends JpaDsl<DictType> {
    QDictType q = QDictType.dictType;

    @Data
    @Accessors(chain = true)
    class Query extends DslQuery<DictType> {

    }

    default Page<DictType> list(QueryRequest<DictTypeDTO.Query> request) {
        return page(new Query().external(request));
    }

    default Optional<DictType> getType(String id) {
        Query query = new Query();
        query.id = id;

        return load(query, q.status.eq(EntityStatus.ENABLE));
    }

}