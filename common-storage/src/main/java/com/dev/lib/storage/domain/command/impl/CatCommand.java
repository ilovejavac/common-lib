package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.core.VfsFileService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Cat 命令 - 读取文件内容
 */
@RequiredArgsConstructor
public class CatCommand implements VfsCommand {

    private final VfsFileService fileService;
    private final String path;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        // 直接返回文件流，不加载到内存
        return fileService.openStream(ctx, path);
    }

    @Override
    public boolean requiresInput() {
        return false;
    }
}
