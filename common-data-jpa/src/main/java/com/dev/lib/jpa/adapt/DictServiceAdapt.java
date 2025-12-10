package com.dev.lib.jpa.adapt;

import com.dev.lib.dict.domain.adapter.DictAdapt;
import com.dev.lib.dict.domain.model.dto.DictItemDTO;
import com.dev.lib.dict.domain.model.dto.DictItemDTO$CreateItemToDictItemEntityMapper;
import com.dev.lib.dict.domain.model.dto.DictItemDTO$UpdateItemToDictItemEntityMapper;
import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.dict.domain.model.dto.DictTypeDTO$CreateTypeToDictTypeMapper;
import com.dev.lib.dict.domain.model.dto.DictTypeDTO$UpdateTypeToDictTypeMapper;
import com.dev.lib.dict.domain.model.valobj.DictItemVO;
import com.dev.lib.dict.domain.model.valobj.DictTypeVO;
import com.dev.lib.dict.serialize.DictItem;
import com.dev.lib.jpa.infra.dict.DictItemEntity;
import com.dev.lib.jpa.infra.dict.DictItemEntityToDictItemMapper;
import com.dev.lib.jpa.infra.dict.DictItemEntityToDictItemVOMapper;
import com.dev.lib.jpa.infra.dict.DictItemRepository;
import com.dev.lib.jpa.infra.dict.DictType;
import com.dev.lib.jpa.infra.dict.DictTypeRepository;
import com.dev.lib.jpa.infra.dict.DictTypeToDictTypeVOMapper;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DictServiceAdapt implements DictAdapt {
    private final DictItemRepository itemRepository;
    private final DictTypeRepository typeRepository;

    private final DictTypeDTO$CreateTypeToDictTypeMapper createTypeMapper;
    private final DictTypeDTO$UpdateTypeToDictTypeMapper updateTypeMapper;

    private final DictItemDTO$CreateItemToDictItemEntityMapper createItemMapper;
    private final DictItemDTO$UpdateItemToDictItemEntityMapper updateItemMapper;

    private final DictItemEntityToDictItemMapper mapper;
    private final DictTypeToDictTypeVOMapper typeVOMapper;
    private final DictItemEntityToDictItemVOMapper itemVOMapper;

    @Override
    public String createType(DictTypeDTO.CreateType cmd) {
        DictType type = createTypeMapper.convert(cmd);
        typeRepository.save(type);

        return type.getBizId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(String id) {
        Optional<DictType> loadedType = typeRepository.getType(id);
        loadedType.ifPresent(typeRepository::delete);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(DictTypeDTO.UpdateType cmd) {
        Optional<DictType> loadedType = typeRepository.getType(cmd.getBizId());
        loadedType.ifPresent(it -> updateTypeMapper.convert(cmd, it));
    }

    @Override
    @Transactional(readOnly = true)
    public ServerResponse<List<DictTypeVO>> pageType(QueryRequest<DictTypeDTO.Query> request) {
        Slice<DictType> page = typeRepository.list(request);

        return ServerResponse.success(page, typeVOMapper::convert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(String id, DictItemDTO.CreateItem cmd) {
        Optional<DictType> loadedType = typeRepository.getType(id);
        loadedType.ifPresent(it -> it.addItem(createItemMapper.convert(cmd)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(DictItemDTO.UpdateItem cmd) {
        Optional<DictItemEntity> loadedItem = itemRepository.getByBizId(cmd.getBizId());
        loadedItem.ifPresent(it -> updateItemMapper.convert(cmd, it));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(String id) {
        Optional<DictItemEntity> loadedItem = itemRepository.getByBizId(id);
        loadedItem.ifPresent(itemRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerResponse<List<DictItemVO>> pageItem(String id, QueryRequest<DictItemDTO.Query> request) {
        Slice<DictItemEntity> page = itemRepository.list(id, request);

        return ServerResponse.success(page, itemVOMapper::convert);
    }

    @Override
    public DictItem getItem(String code) {
        return mapper.convert(itemRepository.getItem(code));
    }

    @Override
    public Collection<DictItem> listItem(Collection<String> codes) {
        return itemRepository.listItem(codes).stream().map(mapper::convert).toList();
    }
}
