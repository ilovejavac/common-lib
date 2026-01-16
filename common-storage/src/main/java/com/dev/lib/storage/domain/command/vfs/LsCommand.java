package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.util.List;

/**
 * ls 命令
 * 支持: -a 显示隐藏文件, -R/-r 递归, -d 只显示目录本身, --depth n 递归深度
 */
public class LsCommand extends VfsCommandBase<List<VfsNode>> {

    public LsCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public List<VfsNode> execute(ExecuteContext ctx) {

        String[]   args   = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        String path = parsed.getString(0);
        if (path == null) path = "";

        // -a: 显示隐藏文件
        boolean showHidden = parsed.hasFlag("a");

        // -d: 只显示目录本身，不列出内容
        boolean dirOnly = parsed.hasFlag("d");

        // -R/-r: 递归列出
        boolean recursive = parsed.hasFlag("R") || parsed.hasFlag("r");

        // --depth: 递归深度（与 -R 配合使用，单独使用时默认 depth=1）
        Integer depth = parsed.getInt("depth", recursive ? 3 : 1);

        var vfsCtx = toVfsContext(ctx);
        vfsCtx.setShowHidden(showHidden);

        if (dirOnly) {
            // -d 模式：只返回目录本身的信息，depth=0
            return vfs.listDirectory(vfsCtx, path, 0);
        }

        return vfs.listDirectory(vfsCtx, path, depth);
    }

}
