package com.dev.lib.ai.service.agent.support.tool;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ls 工具 - 列出目录内容
 * 对应 Claude Code 的 Ls 工具
 */
@Component
@RequiredArgsConstructor
public class LsTool {

    private final VirtualFileSystem vfs;

    /**
     * 列出目录内容
     * @param path 目录路径
     * @param depth 递归深度（1 表示只列出当前目录）
     */
    public List<VfsNode> list(VfsContext ctx, String path, int depth) {
        return vfs.listDirectory(ctx, path, depth);
    }

    /**
     * 列出目录内容（默认深度 1）
     */
    public List<VfsNode> list(VfsContext ctx, String path) {
        return list(ctx, path, 1);
    }

    /**
     * 获取单个文件/目录信息（depth=0）
     */
    public VfsNode info(VfsContext ctx, String path) {
        List<VfsNode> result = vfs.listDirectory(ctx, path, 0);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 检查路径是否存在
     */
    public boolean exists(VfsContext ctx, String path) {
        return vfs.exists(ctx, path);
    }

    /**
     * 检查是否为目录
     */
    public boolean isDirectory(VfsContext ctx, String path) {
        return vfs.isDirectory(ctx, path);
    }
}
