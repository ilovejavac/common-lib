package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.Storage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 链式存储服务接口 - 支持动态 bucket
 *
 * <p>动态传入 bucket，供 Storage 静态工具调用。</p>
 */
public interface ChainStorageService {

    /**
     * 上传文件
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param file       MultipartFile 文件
     * @return SysFile 的 bizId
     * @throws IOException 上传失败
     */
    String upload(String bucketName, String objectKey, MultipartFile file) throws IOException;

    /**
     * 上传文件
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param inputStream 输入流
     * @return SysFile 的 bizId
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
     * @return SysFile 的 bizId
     * @throws IOException 复制失败
     */
    String copy(String bucketName, String sourceKey, String targetKey) throws IOException;

    /**
     * 追加内容
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param content    追加的内容
     * @return SysFile 的 bizId
     * @throws IOException 追加失败
     */
    String append(String bucketName, String objectKey, String content) throws IOException;

    /**
     * 追加字节数组
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param bytes      追加的字节数组
     * @return SysFile 的 bizId
     * @throws IOException 追加失败
     */
    String appendBytes(String bucketName, String objectKey, byte[] bytes) throws IOException;

    /**
     * 写入字符串（覆盖/新建）
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param content    写入的内容
     * @return SysFile 的 bizId
     * @throws IOException 写入失败
     */
    String write(String bucketName, String objectKey, String content) throws IOException;

    /**
     * 写入字节数组（覆盖/新建）
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param bytes      写入的字节数组
     * @return SysFile 的 bizId
     * @throws IOException 写入失败
     */
    String writeBytes(String bucketName, String objectKey, byte[] bytes) throws IOException;

    /**
     * 按行替换文件内容
     *
     * @param bucketName  桶名称
     * @param objectKey   对象键
     * @param transformer 行转换器
     * @return SysFile 的 bizId
     * @throws IOException 替换失败
     */
    String replaceLines(String bucketName, String objectKey, Storage.LineTransformer transformer) throws IOException;

    // ==================== 纯 I/O 操作（不同步 DB） ====================
    // 供 VFS 层使用，VFS 自行管理 SysFile 记录

    /**
     * 写入对象（纯 I/O）
     */
    void putObject(String bucketName, String objectKey, InputStream input) throws IOException;

    /**
     * 复制对象（纯 I/O）
     */
    void copyObject(String bucketName, String sourceKey, String targetKey) throws IOException;

    /**
     * 删除对象（纯 I/O）
     */
    void removeObject(String bucketName, String objectKey) throws IOException;

    /**
     * 追加内容到对象（纯 I/O）
     */
    void appendObject(String bucketName, String objectKey, byte[] content) throws IOException;
}
