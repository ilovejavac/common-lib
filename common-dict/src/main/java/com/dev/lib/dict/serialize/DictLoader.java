package com.dev.lib.dict.serialize;

import com.dev.lib.dict.domain.service.DictService;
import com.dev.lib.web.serialize.PopulateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 用户信息加载器
 * Bean 名称 "userLoader" 对应注解中的 loader 值
 */
@Component("dictLoader")
@RequiredArgsConstructor
public class DictLoader implements PopulateLoader<String, DictItem> {

    private final DictService dictService;

    @Override
    public Map<String, DictItem> batchLoad(Set<String> ids) {

        return dictService.getItems(ids);
    }

}