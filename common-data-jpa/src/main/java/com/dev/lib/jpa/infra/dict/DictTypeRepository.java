package com.dev.lib.jpa.infra.dict;

import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.web.model.QueryRequest;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends BaseRepository<DictType> {
    QDictType q = QDictType.dictType;

    @Data
    @Accessors(chain = true)
    class Query extends DslQuery<DictType> {

    }

    default Slice<DictType> list(QueryRequest<DictTypeDTO.Query> request) {
        return page(new Query().external(request));
    }

    default Optional<DictType> getType(String id) {
        Query query = new Query();
        query.bizId = id;

        return load(query);
    }

}