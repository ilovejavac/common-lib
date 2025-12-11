package com.dev.lib.dict.domain.adapter;

import com.dev.lib.dict.domain.model.dto.DictItemDTO;
import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.dict.domain.model.valobj.DictItemVO;
import com.dev.lib.dict.domain.model.valobj.DictTypeVO;
import com.dev.lib.dict.serialize.DictItem;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;

import java.util.Collection;
import java.util.List;

public interface DictAdapt {

    String createType(DictTypeDTO.CreateType cmd);

    void deleteType(String id);

    void updateType(DictTypeDTO.UpdateType cmd);

    ServerResponse<List<DictTypeVO>> pageType(QueryRequest<DictTypeDTO.Query> request);

    void addItem(String id, DictItemDTO.CreateItem cmd);

    void updateItem(DictItemDTO.UpdateItem cmd);

    void removeItem(String id);

    ServerResponse<List<DictItemVO>> pageItem(String id, QueryRequest<DictItemDTO.Query> request);

    DictItem getItem(String code);

    Collection<DictItem> listItem(Collection<String> codes);

}
