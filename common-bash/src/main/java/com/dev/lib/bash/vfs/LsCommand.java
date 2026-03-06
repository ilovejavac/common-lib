package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsNode;

import java.util.List;

/**
 * ls 命令
 * 支持: -a 显示隐藏文件, -R/-r 递归, -d 只显示目录本身, --depth n 递归深度
 */
public class LsCommand extends VfsCommand<List<VfsNode>> {

    @Override
    public List<VfsNode> execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        String path = parsed.getString(0);
        if (path == null) path = "";

        boolean showHidden = parsed.hasFlag("a");
        boolean dirOnly = parsed.hasFlag("d");
        boolean recursive = parsed.hasFlag("R") || parsed.hasFlag("r");
        Integer depth = parsed.getInt("depth", recursive ? 3 : 1);

        var vfsCtx = toVfsContext(ctx);
        vfsCtx.setShowHidden(showHidden);

        if (dirOnly) {
            return Vfs.listDirectory(vfsCtx, path, 0);
        }

        return Vfs.listDirectory(vfsCtx, path, depth);
    }
}
