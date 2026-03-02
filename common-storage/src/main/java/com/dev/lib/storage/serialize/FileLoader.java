package com.dev.lib.storage.serialize;

import com.dev.lib.storage.domain.adapter.StorageFileRepo;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageFileToFileItemMapper;
import com.dev.lib.web.serialize.PopulateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户信息加载器
 * Bean 名称 "userLoader" 对应注解中的 loader 值
 */
@Component("fileLoader")
@RequiredArgsConstructor
public class FileLoader implements PopulateLoader<String, FileItem> {

    private final StorageFileRepo storageFileRepo;

    private final StorageFileToFileItemMapper mapper;

    @Override
    public Map<String, FileItem> batchLoad(Set<String> ids) {

        List<StorageFile> files = storageFileRepo.findByIds(ids);
        return files.stream().collect(Collectors.toMap(
                StorageFile::getBizId,
                mapper::convert
        ));
    }

}
