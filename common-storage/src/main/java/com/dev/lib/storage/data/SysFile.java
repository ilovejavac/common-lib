package com.dev.lib.storage.data;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageType;
import com.dev.lib.storage.serialize.FileItem;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_file")
@Data
@AutoMappers({
        @AutoMapper(target = FileItem.class, reverseConvertGenerate = false),
        @AutoMapper(target = StorageFile.class)
})
public class SysFile extends JpaEntity {

    @Column(nullable = false)
    private String originalName;    // 原始文件名

    @Column(nullable = false, length = 50)
    private String storageName;     // 存储文件名

    private String        storagePath;     // 存储路径

    private String        url;             // 访问URL

    @Column(length = 20)
    private String        extension;       // 扩展名

    private String        contentType;     // MIME类型

    private Long          size;              // 文件大小(字节)

    private StorageType   storageType;     // 存储类型

    @Column(columnDefinition = "text", unique = true, nullable = false)
    private String        md5;             // MD5值(去重)

    private String        category;        // 分类(avatar/document/image)

    private Boolean       temporary = false; // 临时文件

    private LocalDateTime expirationAt; // 过期时间

}