package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.serialize.FileItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface FileService {

    FileItem getItem(String value);

    StorageFile upload(MultipartFile file, String category) throws IOException;

    StorageFile getById(String id);

    InputStream download(StorageFile sf) throws IOException;

    void delete(StorageFile sf);
}
