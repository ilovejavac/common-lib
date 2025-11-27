package com.dev.lib.storage.domain.adapter;

import com.dev.lib.storage.domain.model.StorageFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StorageFileRepo {
    StorageFile findByBizId(String value);

    void remove(String bizId);

    Optional<StorageFile> findByMd5(String md5);

    void saveFile(StorageFile storageFile);

    List<StorageFile> findByIds(Collection<String> ids);
}
