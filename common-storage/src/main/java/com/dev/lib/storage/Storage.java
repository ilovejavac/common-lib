package com.dev.lib.storage;

import com.dev.lib.storage.domain.service.chain.ChainStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * 存储 API - 链式调用入口
 *
 * <p>参考 RedisCache 和 RedisDistributedLock 的设计模式，提供流式 API</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 获取路径
 * String path = Storage.bucket("my-bucket").object("file.txt").path();
 *
 * // 读取文件内容（注意：会加载到内存，适合小文件）
 * String content = Storage.bucket("my-bucket").object("file.txt").read();
 *
 * // 获取文件流（流式读取，适合大文件）
 * InputStream is = Storage.bucket("my-bucket").object("file.txt").download();
 *
 * // 写入字符串（覆盖/新建）
 * Storage.bucket("my-bucket").object("data.json").write("{\"key\": \"value\"}");
 *
 * // 写入字节数组（覆盖/新建）
 * Storage.bucket("my-bucket").object("data.bin").write(bytes);
 *
 * // 写入输入流（覆盖/新建）
 * Storage.bucket("my-bucket").object("stream.bin").write(inputStream);
 *
 * // 写入 MultipartFile（覆盖/新建）
 * Storage.bucket("my-bucket").object("upload.txt").write(multipartFile);
 *
 * // 下载文件
 * InputStream is = Storage.bucket("my-bucket").object("path/to/file.txt").download();
 *
 * // 获取预签名 URL
 * String url = Storage.bucket("my-bucket").object("path/to/file.txt").presignedUrl(3600);
 *
 * // 删除文件
 * Storage.bucket("my-bucket").object("path/to/file.txt").delete();
 *
 * // 复制文件
 * Storage.bucket("my-bucket").object("source.txt").copy("target.txt");
 *
 * // 追加字符串
 * Storage.bucket("my-bucket").object("log.txt").append("new line\n");
 *
 * // 追加字节数组
 * Storage.bucket("my-bucket").object("log.bin").append(bytes);
 *
 * // 按行替换
 * Storage.bucket("my-bucket").object("data.txt").replaceLines((lineNum, line) -> ...);
 *
 * // 批量删除
 * Storage.batch("my-bucket").deleteAll(List.of("file1.txt", "file2.txt"));
 * }</pre>
 */
@Slf4j
@Component
@ConditionalOnBean(ChainStorageService.class)
@RequiredArgsConstructor
public class Storage implements InitializingBean {

    private final ChainStorageService chainStorageService;

    private static Storage instance;

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    /**
     * 入口方法 - 指定桶名称
     *
     * @param bucketName 桶名称
     * @return BucketBuilder
     */
    public static BucketBuilder bucket(String bucketName) {
        return new BucketBuilder(bucketName);
    }

    /**
     * 批量操作入口
     *
     * @param bucketName 桶名称
     * @return BatchBuilder
     */
    public static BatchBuilder batch(String bucketName) {
        return new BatchBuilder(bucketName);
    }

    /**
     * 桶构建器
     */
    public static class BucketBuilder {

        private final String bucketName;

        BucketBuilder(String bucketName) {
            if (bucketName == null || bucketName.isBlank()) {
                throw new IllegalArgumentException("bucketName must not be empty");
            }
            this.bucketName = bucketName;
        }

        /**
         * 指定对象键（文件路径）
         *
         * @param objectKey 对象键
         * @return ObjectBuilder
         */
        public ObjectBuilder object(String objectKey) {
            if (objectKey == null || objectKey.isBlank()) {
                throw new IllegalArgumentException("objectKey must not be empty");
            }
            // 去掉开头的斜杠
            if (objectKey.startsWith("/")) {
                objectKey = objectKey.substring(1);
            }
            String bucketPrefix = bucketName + "/";
            if (objectKey.startsWith(bucketPrefix)) {
                objectKey = objectKey.substring(bucketPrefix.length());
            }
            return new ObjectBuilder(bucketName, objectKey);
        }
    }

    /**
     * 对象构建器 - 支持链式调用
     */
    public static class ObjectBuilder {

        private final String bucketName;
        private final String objectKey;

        ObjectBuilder(String bucketName, String objectKey) {
            if (objectKey == null || objectKey.isBlank()) {
                throw new IllegalArgumentException("objectKey must not be empty");
            }
            this.bucketName = bucketName;
            this.objectKey = objectKey;
        }

        // ========== 便捷方法 ==========

        /**
         * 获取完整路径
         *
         * @return bucketName/objectKey
         */
        public String path() {
            return bucketName + "/" + objectKey;
        }

        /**
         * 读取文件内容为字符串（UTF-8）
         *
         * @return 文件内容
         * @throws java.io.IOException 读取失败
         */
        public String read() throws java.io.IOException {
            return read(java.nio.charset.StandardCharsets.UTF_8);
        }

        /**
         * 读取文件内容为字符串
         *
         * @param charset 字符集
         * @return 文件内容
         * @throws java.io.IOException 读取失败
         */
        public String read(Charset charset) throws java.io.IOException {
            try (InputStream is = download()) {
                return new String(is.readAllBytes(), charset);
            }
        }

        // ========== 写入操作 ==========

        /**
         * 写入 MultipartFile（覆盖/新建）
         *
         * @param file MultipartFile 文件
         * @return SysFile 的 bizId
         * @throws java.io.IOException 写入失败
         */
        public String write(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
            log.debug("Writing file to bucket: {}, key: {}", bucketName, objectKey);
            return instance.chainStorageService.upload(bucketName, objectKey, file);
        }

        /**
         * 写入输入流（覆盖/新建）
         *
         * @param inputStream 输入流
         * @return SysFile 的 bizId
         * @throws java.io.IOException 写入失败
         */
        public String write(InputStream inputStream) throws java.io.IOException {
            log.debug("Writing stream to bucket: {}, key: {}", bucketName, objectKey);
            return instance.chainStorageService.upload(bucketName, objectKey, inputStream);
        }

        /**
         * 写入字符串（覆盖/新建）
         *
         * @param content 字符串内容
         * @return SysFile 的 bizId
         * @throws java.io.IOException 写入失败
         */
        public String write(String content) throws java.io.IOException {
            log.debug("Writing string to bucket: {}, key: {}", bucketName, objectKey);
            return instance.chainStorageService.write(bucketName, objectKey, content);
        }

        /**
         * 写入字节数组（覆盖/新建）
         *
         * @param bytes 字节数组
         * @return SysFile 的 bizId
         * @throws java.io.IOException 写入失败
         */
        public String write(byte[] bytes) throws java.io.IOException {
            log.debug("Writing bytes to bucket: {}, key: {}, size: {}", bucketName, objectKey, bytes.length);
            return instance.chainStorageService.writeBytes(bucketName, objectKey, bytes);
        }

        /**
         * 追加字符串到文件
         *
         * @param content 追加的内容
         * @return SysFile 的 bizId
         * @throws java.io.IOException 追加失败
         */
        public String append(String content) throws java.io.IOException {
            log.debug("Appending content to bucket: {}, key: {}", bucketName, objectKey);
            return instance.chainStorageService.append(bucketName, objectKey, content);
        }

        /**
         * 追加字节数组到文件
         *
         * @param bytes 追加的字节数组
         * @return SysFile 的 bizId
         * @throws java.io.IOException 追加失败
         */
        public String append(byte[] bytes) throws java.io.IOException {
            log.debug("Appending bytes to bucket: {}, key: {}, size: {}", bucketName, objectKey, bytes.length);
            return instance.chainStorageService.appendBytes(bucketName, objectKey, bytes);
        }

        // ========== 下载操作 ==========

        /**
         * 下载文件
         *
         * @return 输入流
         * @throws java.io.IOException 下载失败
         */
        public InputStream download() throws java.io.IOException {
            log.debug("Downloading file from bucket: {}, key: {}", bucketName, objectKey);
            return instance.chainStorageService.download(bucketName, objectKey);
        }

        // ========== 其他操作 ==========

        /**
         * 删除文件
         */
        public void delete() {
            log.debug("Deleting file in bucket: {}, key: {}", bucketName, objectKey);
            instance.chainStorageService.delete(bucketName, objectKey);
        }

        /**
         * 获取预签名 URL
         *
         * @param expireSeconds 过期时间（秒）
         * @return 预签名 URL
         */
        public String presignedUrl(int expireSeconds) {
            log.debug("Generating presigned URL for bucket: {}, key: {}, expire: {}s", bucketName, objectKey, expireSeconds);
            return instance.chainStorageService.getPresignedUrl(bucketName, objectKey, expireSeconds);
        }

        /**
         * 复制文件到目标路径
         *
         * @param targetObjectKey 目标对象键
         * @return SysFile 的 bizId
         * @throws java.io.IOException 复制失败
         */
        public String copy(String targetObjectKey) throws java.io.IOException {
            log.debug("Copying file from bucket: {}, key: {} to target: {}", bucketName, objectKey, targetObjectKey);
            return instance.chainStorageService.copy(bucketName, objectKey, targetObjectKey);
        }

        /**
         * 按行替换文件内容
         *
         * @param transformer 行转换器
         * @return SysFile 的 bizId
         * @throws java.io.IOException 替换失败
         */
        public String replaceLines(LineTransformer transformer) throws java.io.IOException {
            log.debug("Replacing lines in bucket: {}, key: {}", bucketName, objectKey);
            return instance.chainStorageService.replaceLines(bucketName, objectKey, transformer);
        }
    }

    @FunctionalInterface
    public interface LineTransformer {

        String transform(int lineNum, String line);

    }

    /**
     * 批量操作构建器
     */
    public static class BatchBuilder {

        private final String bucketName;

        BatchBuilder(String bucketName) {
            if (bucketName == null || bucketName.isBlank()) {
                throw new IllegalArgumentException("bucketName must not be empty");
            }
            this.bucketName = bucketName;
        }

        /**
         * 批量删除文件
         *
         * @param objectKeys 对象键集合
         */
        public void deleteAll(Collection<String> objectKeys) {
            if (objectKeys == null || objectKeys.isEmpty()) {
                return;
            }
            log.debug("Batch deleting {} files in bucket: {}", objectKeys.size(), bucketName);
            for (String objectKey : objectKeys) {
                instance.chainStorageService.delete(bucketName, objectKey);
            }
        }
    }
}
