package com.dev.lib.storage.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VfsNode {

    private String id;

    private String name;           // 文件/目录名

    private String path;           // 相对路径

    private Boolean isDirectory;

    private Long size;             // 文件大小，目录为 null

    private String extension;

    private LocalDateTime modifiedAt;

    private List<VfsNode> children;  // 子节点（depth > 0 时填充）

}
