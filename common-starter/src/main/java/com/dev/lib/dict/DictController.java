package com.dev.lib.dict;


import com.dev.lib.dict.model.dto.DictItemDTO$CreateItemToDictItemEntityMapper;
import com.dev.lib.dict.model.dto.DictItemDTO$UpdateItemToDictItemEntityMapper;
import com.dev.lib.dict.model.dto.DictTypeDTO$CreateTypeToDictTypeMapper;
import com.dev.lib.dict.model.dto.DictTypeDTO$UpdateTypeToDictTypeMapper;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.jpa.data.DictItemEntityToDictItemVOMapper;
import com.dev.lib.jpa.data.DictTypeToDictTypeVOMapper;
import com.dev.lib.web.security.annotation.Anonymous;
import com.dev.lib.jpa.data.DictItemEntity;
import com.dev.lib.jpa.data.DictItemRepository;
import com.dev.lib.jpa.data.DictType;
import com.dev.lib.jpa.data.DictTypeRepository;
import com.dev.lib.dict.model.dto.DictItemDTO;
import com.dev.lib.dict.model.dto.DictTypeDTO;
import com.dev.lib.dict.model.valobj.DictItemVO;
import com.dev.lib.dict.model.valobj.DictTypeVO;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 字典接口
 */
@Anonymous
@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class DictController {
    private final DictTypeDTO$CreateTypeToDictTypeMapper createTypeMapper;
    private final DictTypeDTO$UpdateTypeToDictTypeMapper updateTypeMapper;

    private final DictItemDTO$CreateItemToDictItemEntityMapper createItemMapper;
    private final DictItemDTO$UpdateItemToDictItemEntityMapper updateItemMapper;

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository itemRepository;


    /**
     * 创建分类
     */
    @PostMapping("/api/dict/type/create")
    @Transactional(rollbackFor = Exception.class)
    public ServerResponse<String> createType(@RequestBody DictTypeDTO.CreateType cmd) {
        DictType type = createTypeMapper.convert(cmd);
        dictTypeRepository.save(type);

        return ServerResponse.success(type.getBizId());
    }

    /**
     * 删除分类
     */
    @PostMapping("/api/dict/type/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ServerResponse<Void> deleteType(@PathVariable String id) {
        Optional<DictType> loadedType = dictTypeRepository.getType(id);
        loadedType.ifPresent(dictTypeRepository::delete);

        return ServerResponse.ok();
    }

    /**
     * 修改分类
     */
    @PostMapping("/api/dict/type/update")
    @Transactional(rollbackFor = Exception.class)
    public ServerResponse<Void> updateType(@RequestBody DictTypeDTO.UpdateType cmd) {
        Optional<DictType> loadedType = dictTypeRepository.getType(cmd.getBizId());
        loadedType.ifPresent(it -> updateTypeMapper.convert(cmd, it));

        return ServerResponse.ok();
    }

    private final DictTypeToDictTypeVOMapper typeVOMapper;

    /**
     * 查询分类
     */
    @PostMapping("/api/dict/type/query-list")
    public ServerResponse<List<DictTypeVO>> listType(@RequestBody QueryRequest<DictTypeDTO.Query> request) {
        Page<DictType> page = dictTypeRepository.list(request);

        return ServerResponse.success(page, typeVOMapper::convert);
    }

    /**
     * 添加字典项
     */
    @PostMapping("/api/dict/type/{id}/add-item")
    @Transactional(rollbackFor = Exception.class)
    public ServerResponse<Void> addItem(@RequestBody DictItemDTO.CreateItem cmd, @PathVariable String id) {
        Optional<DictType> loadedType = dictTypeRepository.getType(id);
        loadedType.ifPresent(it -> it.addItem(createItemMapper.convert(cmd)));

        return ServerResponse.ok();
    }

    /**
     * 修改字典项
     */
    @PostMapping("/api/dict/item/update-item")
    @Transactional(rollbackFor = Exception.class)
    public ServerResponse<Void> updateItem(@RequestBody DictItemDTO.UpdateItem cmd) {
        Optional<DictItemEntity> loadedItem = itemRepository.getByBizId(cmd.getBizId());
        loadedItem.ifPresent(it -> updateItemMapper.convert(cmd, it));

        return ServerResponse.ok();
    }

    /**
     * 删除字典项
     */
    @PostMapping("/api/dict/item/remove-item")
    @Transactional(rollbackFor = Exception.class)
    public ServerResponse<Void> removeItem(@RequestParam("id") String id) {
        Optional<DictItemEntity> loadedItem = itemRepository.getByBizId(id);
        loadedItem.ifPresent(itemRepository::delete);

        return ServerResponse.ok();
    }

    private final DictItemEntityToDictItemVOMapper itemVOMapper;

    /**
     * 查询字典项
     */
    @PostMapping("/api/dict/type/{id}/query-item")
    public ServerResponse<List<DictItemVO>> listItem(
            @PathVariable String id,
            @RequestBody QueryRequest<DictItemDTO.Query> request
    ) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(4101, "请指定字典类型 id");
        }
        Page<DictItemEntity> page = itemRepository.list(id, request);

        return ServerResponse.success(page, itemVOMapper::convert);
    }

}