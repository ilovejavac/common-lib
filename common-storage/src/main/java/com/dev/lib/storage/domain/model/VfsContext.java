package com.dev.lib.storage.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VfsContext {

    private String  root;  // 如 "/a"，null 表示租户根目录

    private boolean showHidden = false;  // 是否显示隐藏文件（以.开头）

    /**
     * 服务归属，未设置时默认使用 spring.application.name
     */
    private String serviceName;

    /**
     * 临时文件标记：
     * - null: 不改变已有文件的临时属性（用于覆盖写场景）
     * - true/false: 显式设置
     */
    private Boolean temporary;

    /**
     * 临时文件过期时间（temporary=true 时生效）
     * 为空时由系统按默认 TTL 推导
     */
    private LocalDateTime expirationAt;

    public static VfsContext of(String root) {

        VfsContext ctx = new VfsContext();
        ctx.setRoot(root);
        return ctx;
    }

}
