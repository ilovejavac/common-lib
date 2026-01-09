package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * mv 命令
 */
public class MvCommand extends VfsCommandBase {

    public MvCommand(VirtualFileSystem vfs) {
        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("mv: missing destination file operand");
        }

        if (parsed.positionalCount() == 2) {
            String src = parsed.getString(0);
            String dest = parsed.getString(1);
            vfs.move(toVfsContext(ctx), src, dest);
        } else {
            String destDir = parsed.getString(parsed.positionalCount() - 1);
            for (int i = 0; i < parsed.positionalCount() - 1; i++) {
                String src = parsed.getString(i);
                vfs.move(toVfsContext(ctx), src, destDir);
            }
        }

        return null;
    }
}
