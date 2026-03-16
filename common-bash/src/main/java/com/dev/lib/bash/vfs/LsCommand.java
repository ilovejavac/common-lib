package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsNode;

import java.util.List;

/**
 * ls 命令 - 列出目录内容
 * 支持: -a 显示隐藏文件, -R/-r 递归, -d 只显示目录本身
 */
public class LsCommand extends VfsCommand<List<VfsNode>> {

    @Override
    public List<VfsNode> execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        String path = parsed.getString(0);
        if (path == null) path = "";

        boolean showHidden = parsed.hasFlag("a");

        var vfsCtx = toVfsContext(ctx);
        return Vfs.path(vfsCtx, path).ls(showHidden);
    }
}
