package com.dev.lib.dict.trigger;


import com.dev.lib.dict.domain.adapter.DictAdapt;
import com.dev.lib.dict.domain.model.dto.DictItemDTO;
import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.dict.domain.model.valobj.DictItemVO;
import com.dev.lib.dict.domain.model.valobj.DictTypeVO;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.annotation.Anonymous;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典接口
 */
@Anonymous
@RestController
@RequiredArgsConstructor
public class DictController {
    @Resource
    private DictAdapt adapt;

    /**
     * 创建分类
     */
    @PostMapping("/api/dict/type/create")
    public ServerResponse<String> createType(@RequestBody DictTypeDTO.CreateType cmd) {
        return ServerResponse.success(adapt.createType(cmd));
    }

    /**
     * 删除分类
     */
    @PostMapping("/api/dict/type/delete/{id}")
    public ServerResponse<Void> deleteType(@PathVariable String id) {
        adapt.deleteType(id);
        return ServerResponse.ok();
    }

    /**
     * 修改分类
     */
    @PostMapping("/api/dict/type/update")
    public ServerResponse<Void> updateType(@RequestBody DictTypeDTO.UpdateType cmd) {
        adapt.updateType(cmd);

        return ServerResponse.ok();
    }

    /**
     * 查询分类
     */
    @PostMapping("/api/dict/type/query-list")
    public ServerResponse<List<DictTypeVO>> listType(@RequestBody QueryRequest<DictTypeDTO.Query> request) {
        return adapt.pageType(request);
    }

    /**
     * 添加字典项
     */
    @PostMapping("/api/dict/type/{id}/add-item")
    public ServerResponse<Void> addItem(@RequestBody DictItemDTO.CreateItem cmd, @PathVariable String id) {
        adapt.addItem(id, cmd);

        return ServerResponse.ok();
    }

    /**
     * 修改字典项
     */
    @PostMapping("/api/dict/item/update-item")
    public ServerResponse<Void> updateItem(@RequestBody DictItemDTO.UpdateItem cmd) {
        adapt.updateItem(cmd);

        return ServerResponse.ok();
    }

    /**
     * 删除字典项
     */
    @PostMapping("/api/dict/item/remove-item")
    public ServerResponse<Void> removeItem(@RequestParam("id") String id) {
        adapt.removeItem(id);

        return ServerResponse.ok();
    }

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

        return adapt.pageItem(id, request);
    }

}