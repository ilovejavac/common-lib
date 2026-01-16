package com.dev.lib.storage.domain.adapter;

import com.dev.lib.storage.domain.model.StorageFile;

import java.util.Collection;
import java.util.List;

public interface StorageFileRepo {

    StorageFile findByBizId(String value);

    void remove(String bizId);

    void saveFile(StorageFile storageFile);

    List<StorageFile> findByIds(Collection<String> ids);

    Collection<String> collectRemovePath(Collection<String> ids);

    /**
     * 批量删除文件记录
     */
    void removeAllByIds(Collection<String> ids);

}
