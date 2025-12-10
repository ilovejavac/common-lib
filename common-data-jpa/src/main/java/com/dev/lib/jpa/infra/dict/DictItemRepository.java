package com.dev.lib.jpa.infra.dict;

import com.dev.lib.dict.domain.model.dto.DictItemDTO;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.web.model.QueryRequest;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DictItemRepository extends BaseRepository<DictItemEntity> {

    @Data
    class Query extends DslQuery<DictItemEntity> {
        @Condition(type = QueryType.EQ)
        private String itemCode;

        @Condition(type = QueryType.IN, field = "itemCode")
        private Collection<String> itemCodes;

        @Condition(type = QueryType.EQ, field = "dictType.bizId")
        private String type;
    }

    default Slice<DictItemEntity> list(String type, QueryRequest<DictItemDTO.Query> request) {
        return page(new Query().setType(type).external(request));
    }

    Optional<DictItemEntity> getByBizId(String bizId);

//    @org.springframework.data.jpa.repository.Query("select DictItemEntity from DictItemEntity where itemCode = :code")
    default DictItemEntity getItem(String code) {
        return load(new Query().setItemCode(code)).orElse(null);
    }

    default List<DictItemEntity> listItem(Collection<String> codes) {
        return loads(new Query().setItemCodes(codes));
    }

}