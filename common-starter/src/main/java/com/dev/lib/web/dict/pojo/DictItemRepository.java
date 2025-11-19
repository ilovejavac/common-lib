package com.dev.lib.web.dict.pojo;

import com.dev.lib.entity.BaseRepository;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.List;

public interface DictItemRepository extends BaseRepository<DictItemEntity> {

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Accessors(chain = true)
    class Query extends DslQuery<DictItemEntity> {
        @Condition(type = QueryType.EQ)
        private String itemCode;

        @Condition(type = QueryType.IN, field = "itemCode")
        private List<String> itemCodes;
    }

    default DictItemEntity getItem(String code) {
        Query query = new Query().setItemCode(code);
        return load(query).orElse(null);
    }

    default List<DictItemEntity> listItem(Collection<String> codes) {
        Query query = new Query().setItemCodes(codes.stream().toList());
        return loads(query);
    }

}