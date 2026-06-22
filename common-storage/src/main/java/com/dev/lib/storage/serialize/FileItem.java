package com.dev.lib.storage.serialize;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class FileItem {

    @JSONField(name = "id")
    private String bizId;

    private String originalName;    // 原始文件名

    private String extension;       // 扩展名

    private String contentType;     // MIME类型

    private Long size;              // 文件大小(字节)

    private String category;        // 分类(avatar/document/image)

}
