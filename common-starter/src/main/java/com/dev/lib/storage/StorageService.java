package com.dev.lib.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    /**
     * 上传文件
     */
    String upload(MultipartFile file, String path) throws IOException;

    /**
     * 下载文件
     */
    byte[] download(String path) throws IOException;

    /**
     * 删除文件
     */
    void delete(String path);

    /**
     * 获取访问URL
     */
    String getUrl(String path);
}
