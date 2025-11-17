package com.dev.lib.web.dict.pojo;

import com.dev.lib.entity.BaseRepository;
import com.dev.lib.web.dict.model.dto.DictTypeRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends BaseRepository<DictType> {

    default Optional<DictType> getType(String id) {
        DictTypeRequest.GetType getType = new DictTypeRequest.GetType(id);

        return fetchOne(getType);
    }

}