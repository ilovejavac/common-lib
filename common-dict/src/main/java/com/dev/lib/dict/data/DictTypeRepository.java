package com.dev.lib.dict.data;

import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.web.model.QueryRequest;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends BaseRepository<DictType> {

    QDictType q = QDictType.dictType;

    @Data
    @Accessors(chain = true)
    class Query extends DslQuery<DictType> {

    }

    default Page<DictType> list(QueryRequest<DictTypeDTO.Query> request) {

        return page(new Query().external(request));
    }

    default Optional<DictType> getType(String id) {

        return load(new Query().setBizId(id));
    }

}