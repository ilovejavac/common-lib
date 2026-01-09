package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * ls 命令
 * 支持: -d 只显示目录本身, -a 显示隐藏文件, --depth n 递归深度
 */
public class LsCommand extends VfsCommandBase {

    public LsCommand(VirtualFileSystem vfs) {
        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        String path = parsed.getString(0);
        if (path == null) path = "/";

        // -d: 只显示目录本身，不列出内容
        boolean dirOnly = parsed.hasFlag("d");

        // --depth 或自定义深度参数
        Integer depth = parsed.getInt("depth", 1);

        if (dirOnly) {
            // -d 模式：只返回目录本身的信息，depth=0
            return vfs.listDirectory(toVfsContext(ctx), path, 0);
        }

        return vfs.listDirectory(toVfsContext(ctx), path, depth);
    }
}
