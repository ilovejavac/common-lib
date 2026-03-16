package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * mkdir 命令 - 创建目录
 * 支持: -p 递归创建父目录
 */
public class MkdirCommand extends VfsCommand<Void> {

    @Override
    public Void execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);
        boolean createParents = parsed.hasFlag("p") || parsed.hasFlag("r");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("mkdir: missing operand");
        }

        var vfsCtx = toVfsContext(ctx);
        for (int i = 0; i < parsed.positionalCount(); i++) {
            Vfs.path(vfsCtx, parsed.getString(i)).mkdir(createParents);
        }

        return null;
    }
}
