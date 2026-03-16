package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.Set;

/**
 * tree 命令 - 树形显示目录结构
 * 支持: -L depth 限制深度
 */
public class TreeBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("L"), Set.of());

        String path = parsed.getString(0);
        if (path == null) path = "";

        int depth = parsed.getInt("L", 3);

        var vfsCtx = toVfsContext(ctx);
        return Vfs.path(vfsCtx, path).tree(depth);
    }
}
