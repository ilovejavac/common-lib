package com.dev.lib.storage.domain.model;

import com.dev.lib.storage.serialize.FileItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AutoMapper(target = FileItem.class)
public class StorageFile {

    @JsonProperty("id")
    private String bizId;

    private String originalName;    // 原始文件名

    private String storageName;     // 存储文件名

    private String storagePath;     // 存储路径

    private String url;             // 访问URL

    private String extension;       // 扩展名

    private String contentType;     // MIME类型

    private Long size;              // 文件大小(字节)

    @Column(length = 12)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;     // 存储类型

//    private String md5;             // MD5值(去重)

    private String category;        // 分类(avatar/document/image)

    private Boolean temporary = false; // 临时文件

    private LocalDateTime expirationAt; // 过期时间

    private LocalDateTime createAt;

}
