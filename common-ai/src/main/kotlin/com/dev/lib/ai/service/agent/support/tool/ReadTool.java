package com.dev.lib.ai.service.agent.support.tool;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Read 工具 - 读取文件内容
 * 对应 Claude Code 的 Read 工具
 */
@Component
@RequiredArgsConstructor
public class ReadTool {

    private final VirtualFileSystem vfs;

    /**
     * 读取全部内容
     */
    public String readAll(VfsContext ctx, String path) {
        return vfs.readFile(ctx, path);
    }

    /**
     * 按行读取（支持 offset/limit）
     * @param startLine 起始行号（从 1 开始）
     * @param lineCount 读取行数（-1 表示读到末尾）
     */
    public List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount) {
        return vfs.readLines(ctx, path, startLine, lineCount);
    }

    /**
     * 按字节读取
     * @param offset 起始字节位置（从 0 开始）
     * @param limit 读取字节数（-1 表示读到末尾）
     */
    public byte[] readBytes(VfsContext ctx, String path, long offset, int limit) {
        return vfs.readBytes(ctx, path, offset, limit);
    }

    /**
     * 获取文件大小
     */
    public long getFileSize(VfsContext ctx, String path) {
        return vfs.getFileSize(ctx, path);
    }

    /**
     * 获取文件行数
     */
    public int getLineCount(VfsContext ctx, String path) {
        return vfs.getLineCount(ctx, path);
    }
}
