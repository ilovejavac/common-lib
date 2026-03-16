package com.dev.lib.storage.domain.command;

import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * 管道命令 - 连接两个命令
 */
@RequiredArgsConstructor
public class PipeCommand implements VfsCommand {

    private final VfsCommand first;
    private final VfsCommand second;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        InputStream intermediate = first.execute(ctx, input);
        return second.execute(ctx, intermediate);
    }

    @Override
    public boolean requiresInput() {
        return first.requiresInput();
    }
}
