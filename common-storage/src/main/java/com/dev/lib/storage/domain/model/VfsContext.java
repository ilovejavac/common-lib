package com.dev.lib.storage.domain.model;

import lombok.Data;

@Data
public class VfsContext {

    private String root;  // 如 "/a"，null 表示租户根目录

    public static VfsContext of(String root) {

        VfsContext ctx = new VfsContext();
        ctx.setRoot(root);
        return ctx;
    }

}
