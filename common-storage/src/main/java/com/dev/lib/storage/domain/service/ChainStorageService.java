package com.dev.lib.storage.domain.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 链式存储服务接口 - 支持动态 bucket
 *
 * <p>与 StorageService 的区别：</p>
 * <ul>
 *   <li>StorageService: 从配置读取固定 bucket，传统接口</li>
 *   <li>ChainStorageService: 动态传入 bucket，支持链式调用</li>
 * </ul>
 */
public interface ChainStorageService {

    /**
     * 上传文件
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param file       MultipartFile 文件
     * @return 对象键
     * @throws IOException 上传失败
     */
    String upload(String bucketName, String objectKey, MultipartFile file) throws IOException;

    /**
     * 上传文件
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param inputStream 输入流
     * @return 对象键
     * @throws IOException 上传失败
     */
    String upload(String bucketName, String objectKey, InputStream inputStream) throws IOException;

    /**
     * 下载文件
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @return 输入流
     * @throws IOException 下载失败
     */
    InputStream download(String bucketName, String objectKey) throws IOException;

    /**
     * 删除文件
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     */
    void delete(String bucketName, String objectKey);

    /**
     * 获取预签名 URL
     *
     * @param bucketName    桶名称
     * @param objectKey     对象键
     * @param expireSeconds 过期时间（秒）
     * @return 预签名 URL
     */
    String getPresignedUrl(String bucketName, String objectKey, int expireSeconds);

    /**
     * 复制文件
     *
     * @param bucketName 桶名称
     * @param sourceKey  源对象键
     * @param targetKey  目标对象键
     * @return 目标对象键
     * @throws IOException 复制失败
     */
    String copy(String bucketName, String sourceKey, String targetKey) throws IOException;

    /**
     * 追加内容
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param content    追加的内容
     * @return 对象键
     * @throws IOException 追加失败
     */
    String append(String bucketName, String objectKey, String content) throws IOException;

    /**
     * 追加字节数组
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param bytes      追加的字节数组
     * @return 对象键
     * @throws IOException 追加失败
     */
    String appendBytes(String bucketName, String objectKey, byte[] bytes) throws IOException;

    /**
     * 写入字符串（覆盖/新建）
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param content    写入的内容
     * @return 对象键
     * @throws IOException 写入失败
     */
    String write(String bucketName, String objectKey, String content) throws IOException;

    /**
     * 写入字节数组（覆盖/新建）
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param bytes      写入的字节数组
     * @return 对象键
     * @throws IOException 写入失败
     */
    String writeBytes(String bucketName, String objectKey, byte[] bytes) throws IOException;

    /**
     * 按行替换文件内容
     *
     * @param bucketName  桶名称
     * @param objectKey   对象键
     * @param transformer 行转换器
     * @return 对象键
     * @throws IOException 替换失败
     */
    String replaceLines(String bucketName, String objectKey, StorageService.LineTransformer transformer) throws IOException;
}
