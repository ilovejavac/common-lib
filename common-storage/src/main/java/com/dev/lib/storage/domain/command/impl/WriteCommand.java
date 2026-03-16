package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.core.VfsFileService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Write 命令 - 写入文件
 */
@RequiredArgsConstructor
public class WriteCommand implements VfsCommand {

    private final VfsFileService fileService;
    private final String targetPath;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Write requires input stream");
        }

        // 流式写入文件
        fileService.writeStream(ctx, targetPath, input);

        // 写入命令通常是终止命令，返回 null
        return null;
    }

    @Override
    public boolean requiresInput() {
        return true;
    }
}
