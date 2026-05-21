package com.dev.lib.storage.data;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Data;

import java.util.Optional;

/**
 * 基于服务名、桶名和对象键查询文件元数据。
 */
public interface SysFileObjectRepository extends BaseRepository<SysFile> {

    @Data
    class Query extends DslQuery<SysFile> {

        private String serviceName;

        private String bucketName;

        private String objectKey;

    }

    default Optional<SysFile> findByBucketNameAndObjectKey(
            String serviceName,
            String bucketName,
            String objectKey
    ) {

        return load(new Query()
                .setServiceName(serviceName)
                .setBucketName(bucketName)
                .setObjectKey(objectKey));
    }

    default Optional<SysFile> findByBucketNameAndObjectKeyForUpdate(
            String serviceName,
            String bucketName,
            String objectKey
    ) {

        return lockForUpdate().load(new Query()
                .setServiceName(serviceName)
                .setBucketName(bucketName)
                .setObjectKey(objectKey));
    }
}
