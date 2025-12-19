package com.dev.lib.storage.serialize;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

public class FileItem {

    @JsonProperty("id")
    private String bizId;

    private String originalName;    // 原始文件名

    private String        url;             // 访问URL

    private String        extension;       // 扩展名

    private String        contentType;     // MIME类型

    private Long          size;              // 文件大小(字节)

    private String        category;        // 分类(avatar/document/image)

    private Boolean       temporary = false; // 临时文件

    private LocalDateTime expirationAt; // 过期时间

    private LocalDateTime createAt;

}
