package com.dev.lib.storage.data;

import com.dev.lib.jpa.TenantEntity;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageType;
import com.dev.lib.storage.serialize.FileItem;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sys_storage_file",
        uniqueConstraints = @UniqueConstraint(columnNames = {"virtualPath"}),
        indexes = {
                @Index(name = "idx_parent_path", columnList = "parentPath"),
                @Index(name = "idx_virtual_path_prefix", columnList = "virtualPath")
        })
@Data
@AutoMappers({
        @AutoMapper(target = FileItem.class, reverseConvertGenerate = false),
        @AutoMapper(target = StorageFile.class)
})
public class SysFile extends TenantEntity {

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

    private Boolean temporary = false; // 临时文件

    private LocalDateTime expirationAt; // 过期时间

    private String virtualPath;       // 逻辑路径: "/a/d/d3.md"

    private String parentPath;        // 父路径: "/a/d" (加速查询)

    private Boolean isDirectory = false;  // 是否目录

    private Boolean hidden = false;       // 是否隐藏文件（以.开头）

    @Version
    private Long version;             // 乐观锁版本号

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private List<String> oldStoragePaths;    // 旧存储路径（用于延迟删除，FIFO 顺序）

    private LocalDateTime deleteAfter; // 延迟删除时间

}