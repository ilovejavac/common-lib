package com.dev.lib.web.dict;

import com.dev.lib.web.dict.data.DictType;
import com.dev.lib.web.dict.data.DictTypeRepository;
import com.dev.lib.web.dict.model.dto.DictItemDTO;
import com.dev.lib.web.dict.model.dto.DictItemDTOToDictItemEntityMapper;
import com.dev.lib.web.dict.model.dto.DictTypeDTO;
import com.dev.lib.web.dict.model.dto.DictTypeDTOToDictTypeMapper;
import com.dev.lib.web.model.ServerResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 字典接口
 */
@RestController
@RequiredArgsConstructor
public class DictController {

    private final DictTypeDTOToDictTypeMapper mapper;
    private final DictItemDTOToDictItemEntityMapper itemMapper;
    private final DictTypeRepository dictTypeRepository;


    /**
     * 创建分类
     */
    @PostMapping("/api/dict/type/create")
    public ServerResponse<String> createType(@RequestBody DictTypeDTO cmd) {
        DictType type = mapper.convert(cmd);
        dictTypeRepository.save(type);

        return ServerResponse.success(type.getBizId());
    }

    /**
     * 删除分类
     */
    @PostMapping("/api/dict/type/delete/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ServerResponse<Void> deleteType(@PathVariable String id) {
        Optional<DictType> loadedType = dictTypeRepository.getType(id);
        loadedType.ifPresent(dictTypeRepository::delete);
        return ServerResponse.ok();
    }

    /**
     * 修改分类
     */
    @PostMapping("/api/dict/type/update")
    @Transactional(rollbackOn = Exception.class)
    public ServerResponse<Void> updateType(@RequestBody DictTypeDTO cmd) {
        Optional<DictType> loadedType = dictTypeRepository.getType(cmd.getBizId());
        loadedType.ifPresent(it -> mapper.convert(cmd, it));

        return ServerResponse.ok();
    }

    @PostMapping("/api/dict/type/{id}/add-item")
    @Transactional(rollbackOn = Exception.class)
    public ServerResponse<Void> addItem(@RequestBody DictItemDTO cmd, @PathVariable String id) {
        Optional<DictType> loadedType = dictTypeRepository.getType(id);

        loadedType.ifPresent(it -> it.addItem(itemMapper.convert(cmd)));

        return ServerResponse.ok();
    }

}