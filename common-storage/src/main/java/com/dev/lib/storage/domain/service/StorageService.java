package com.dev.lib.storage.domain.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface StorageService {

    /**
     * 行转换器接口，用于逐行处理文件内容
     */
    @FunctionalInterface
    interface LineTransformer {

        /**
         * 转换单行内容
         *
         * @param lineNum 行号（从1开始）
         * @param line    行内容
         * @return 转换后的行内容，返回 null 表示删除该行
         */
        String transform(int lineNum, String line);

    }

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
     * 批量删除文件
     * 使用各存储服务的原生批量删除 API，一次网络调用删除多个文件
     */
    void deleteAll(Collection<String> paths);

    /**
     * 获取临时访问URL（带签名）
     *
     * @param path         文件路径
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    default String getPresignedUrl(String path, int expireSeconds) {
        throw new UnsupportedOperationException("getPresignedUrl not implemented");
    }

    /**
     * 复制文件（存储服务原生实现）
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 目标文件路径
     */
    String copy(String sourcePath, String targetPath) throws IOException;

    /**
     * 追加内容（存储服务原生实现）
     *
     * @param path    文件路径
     * @param content 追加的内容
     * @return 最终文件路径
     */
    String append(String path, String content) throws IOException;

    /**
     * 按行替换文件内容（存储服务原生实现）
     * 逐行处理文件，不经 JVM 内存，支持大文件
     *
     * @param path        文件路径
     * @param transformer 行转换器
     * @return 文件路径
     */
    String replaceLines(String path, LineTransformer transformer) throws IOException;

}
