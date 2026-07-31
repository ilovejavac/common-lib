package com.dev.lib.storage.data;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageType;
import com.dev.lib.storage.serialize.FileItem;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sys_storage_file",
        uniqueConstraints = @UniqueConstraint(columnNames = {"serviceName", "bucketName", "objectKey"}),
        indexes = {
                @Index(name = "idx_service_name", columnList = "serviceName"),
                @Index(name = "idx_service_bucket_object", columnList = "serviceName,bucketName,objectKey")
        })
@Getter
@Setter
@AutoMappers({
        @AutoMapper(target = FileItem.class, reverseConvertGenerate = false),
        @AutoMapper(target = StorageFile.class)
})
public class SysFile extends JpaEntity {

    @Column(nullable = false)
    private String originalName;    // 原始文件名

    @Column(nullable = false, length = 50)
    private String storageName;     // 存储文件名

    private String storagePath;     // 存储路径

    @Column(length = 20)
    private String extension;       // 扩展名

    private String contentType;     // MIME类型

    private Long size;              // 文件大小(字节)

    @Column(length = 12)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;     // 存储类型

    private String category;        // 分类(avatar/document/image)

    @Column(length = 128)
    private String serviceName;     // 服务归属（默认 spring.application.name）

    @Column(nullable = false, length = 128)
    private String bucketName;      // 存储桶名称

    @Column(nullable = false, length = 1024)
    private String objectKey;       // 对象键

    @Version
    private Long version;             // 乐观锁版本号

}
