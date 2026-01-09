package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * rm 命令
 */
public class RmCommand extends VfsCommandBase {

    public RmCommand(VirtualFileSystem vfs) {
        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r");
        boolean force = parsed.hasFlag("f");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("rm: missing operand");
        }

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            try {
                vfs.delete(toVfsContext(ctx), path, recursive);
            } catch (IllegalArgumentException e) {
                // -f 忽略不存在的文件
                if (!force || !e.getMessage().contains("not found")) {
                    throw e;
                }
            }
        }

        return null;
    }
}
