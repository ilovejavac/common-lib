package com.dev.lib.storage.domain.service.virtual.path;

import com.dev.lib.storage.domain.model.VfsContext;
import org.springframework.stereotype.Component;

/**
 * VFS 路径解析器
 * 负责将用户路径解析为 VFS 内部路径
 */
@Component
public class VfsPathResolver {

    /**
     * 解析路径，合并 context 的 root 和用户提供的 path
     */
    public String resolve(VfsContext ctx, String path) {

        return com.dev.lib.storage.domain.service.virtual.VfsPathUtils.resolvePath(ctx.getRoot(), path);
    }

    /**
     * 规范化路径，处理 . 和 .. 以及多余的 /
     */
    public String normalize(String path) {

        return com.dev.lib.storage.domain.service.virtual.VfsPathUtils.normalizePath(path);
    }

    /**
     * 获取父路径
     */
    public String getParent(String virtualPath) {

        return com.dev.lib.storage.domain.service.virtual.VfsPathUtils.getParentPath(virtualPath);
    }

    /**
     * 获取文件/目录名
     */
    public String getName(String virtualPath) {

        return com.dev.lib.storage.domain.service.virtual.VfsPathUtils.getName(virtualPath);
    }

    /**
     * 检查 child 是否是 parent 的子路径（或相同）
     */
    public boolean isSubPath(String parent, String child) {

        return com.dev.lib.storage.domain.service.virtual.VfsPathUtils.isSubPath(parent, child);
    }

    /**
     * 获取文件扩展名（小写）
     */
    public String getExtension(String fileName) {

        return com.dev.lib.storage.domain.service.virtual.VfsPathUtils.getExtension(fileName);
    }

}
