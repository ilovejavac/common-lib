package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * find 命令
 * 语法: find path -name pattern [-maxdepth n]
 */
public class FindCommand extends VfsCommandBase {

    public FindCommand(VirtualFileSystem vfs) {
        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());

        if (args.length == 0) {
            throw new IllegalArgumentException("find: missing path");
        }

        String basePath = args[0];
        String pattern = "*";
        boolean recursive = true;

        for (int i = 1; i < args.length; i++) {
            if ("-name".equals(args[i]) && i + 1 < args.length) {
                pattern = args[++i];
            } else if ("-maxdepth".equals(args[i]) && i + 1 < args.length) {
                int depth = Integer.parseInt(args[++i]);
                recursive = depth > 1;
            }
        }

        return vfs.findByName(toVfsContext(ctx), basePath, pattern, recursive);
    }
}
