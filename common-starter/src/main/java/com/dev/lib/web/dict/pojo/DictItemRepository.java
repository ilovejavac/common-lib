package com.dev.lib.web.dict.pojo;

import com.dev.lib.entity.BaseRepository;
import com.dev.lib.web.dict.model.dto.DictItemRequest;

import java.util.Collection;
import java.util.List;

public interface DictItemRepository extends BaseRepository<DictItemEntity> {

    default DictItemEntity getItem(String code) {
        DictItemRequest.GetItem getItem = new DictItemRequest.GetItem(code);

        return fetchOne(getItem).orElse(null);
    }

    default List<DictItemEntity> listItem(Collection<String> codes) {
        DictItemRequest.ListItem listItem = new DictItemRequest.ListItem(codes.stream().toList());

        return fetch(listItem);
    }

}