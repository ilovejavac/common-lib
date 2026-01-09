package com.dev.lib.ai.service.agent.support.tool

import com.dev.lib.storage.domain.model.VfsContext
import com.dev.lib.storage.domain.model.VfsNode
import com.dev.lib.storage.domain.service.VirtualFileSystem
import org.springframework.stereotype.Component

/**
 * Find 工具 - 查找文件
 * 对应 Claude Code 的 Find 工具
 */
@Component
class FindTool(
    val vfs: VirtualFileSystem
) {

    /**
     * 按文件名模式查找
     * @param basePath 基础路径
     * @param pattern 文件名模式（支持通配符 * 和 ?）
     * @param recursive 是否递归查找
     */
    fun findByName(
        ctx: VfsContext?,
        basePath: String?,
        pattern: String?,
        recursive: Boolean = true
    ): MutableList<VfsNode?>? {
        return vfs.findByName(ctx, basePath, pattern, recursive)
    }

    /**
     * 按内容查找文件
     * @param basePath 基础路径
     * @param content 要搜索的内容
     * @param recursive 是否递归查找
     */
    fun findByContent(
        ctx: VfsContext?,
        basePath: String?,
        content: String?,
        recursive: Boolean = true
    ): MutableList<VfsNode?>? {
        return vfs.findByContent(ctx, basePath, content, recursive)
    }
}
