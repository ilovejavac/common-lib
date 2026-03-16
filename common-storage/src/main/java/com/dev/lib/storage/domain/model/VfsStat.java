package com.dev.lib.storage.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VFS 文件状态信息
 */
@Data
@Builder
public class VfsStat {

    /**
     * 虚拟路径
     */
    private String path;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 是否目录
     */
    private Boolean isDirectory;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
