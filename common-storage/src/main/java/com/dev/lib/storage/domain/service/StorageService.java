package com.dev.lib.storage.domain.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

interface StorageService {

    /**
     * 上传文件
     */
    String upload(MultipartFile file, String path) throws IOException;

    default String upload(InputStream is, String path) throws IOException {

        return null;
    }

    /**
     * 下载文件
     */
    InputStream download(String path) throws IOException;

    /**
     * 删除文件
     */
    void delete(String path);

    /**
     * 获取访问URL
     */
    String getUrl(String path);

}
