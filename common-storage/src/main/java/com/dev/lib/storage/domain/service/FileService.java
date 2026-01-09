package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.serialize.FileItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public interface FileService {

    FileItem getItem(String value);

    StorageFile upload(InputStream is, String category) throws IOException;

    StorageFile upload(MultipartFile file, String category) throws IOException;

    StorageFile getById(String id);

    InputStream download(StorageFile sf) throws IOException;

    /**
     * 批量删除文件
     */
    void deleteAll(Collection<String> ids);

    Map<String, FileItem> getItems(Collection<String> ids);

}
