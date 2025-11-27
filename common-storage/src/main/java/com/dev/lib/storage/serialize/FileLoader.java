package com.dev.lib.storage.serialize;

import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.service.FileService;
import com.dev.lib.web.serialize.PopulateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 用户信息加载器
 * Bean 名称 "userLoader" 对应注解中的 loader 值
 */
@Component("fileLoader")
@RequiredArgsConstructor
public class FileLoader implements PopulateLoader<String, FileItem> {

    private final FileService fileService;

    @Override
    public Map<String, FileItem> batchLoad(Set<String> ids) {
        return fileService.getItems(ids);
    }

    @Override
    public Class<String> keyType() {
        return String.class;
    }
}