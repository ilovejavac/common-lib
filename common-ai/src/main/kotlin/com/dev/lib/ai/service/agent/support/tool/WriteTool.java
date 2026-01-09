package com.dev.lib.ai.service.agent.support.tool;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Write 工具 - 写入文件内容
 * 对应 Claude Code 的 Write 工具
 */
@Component
@RequiredArgsConstructor
public class WriteTool {

    private final VirtualFileSystem vfs;

    /**
     * 写入文件（覆盖模式）
     */
    public void write(VfsContext ctx, String path, String content) {
        vfs.writeFile(ctx, path, content);
    }

    /**
     * 追加内容到文件末尾
     */
    public void append(VfsContext ctx, String path, String content) {
        vfs.appendFile(ctx, path, content);
    }

    /**
     * 创建空文件或更新时间戳
     */
    public void touch(VfsContext ctx, String path) {
        vfs.touchFile(ctx, path);
    }
}
