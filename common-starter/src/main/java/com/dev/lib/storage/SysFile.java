package com.dev.lib.storage;

import com.dev.lib.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_file")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysFile extends BaseEntity {
    private String originalName;    // 原始文件名
    private String storageName;     // 存储文件名
    private String storagePath;     // 存储路径
    private String url;             // 访问URL
    private String extension;       // 扩展名
    private String contentType;     // MIME类型
    private Long size;              // 文件大小(字节)
    private StorageType storageType;     // 存储类型
    private String md5;             // MD5值(去重)
    private String category;        // 分类(avatar/document/image)
    private Boolean temporary = false; // 临时文件
    private LocalDateTime expirationAt; // 过期时间
}