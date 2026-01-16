package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * cp 命令
 */
public class CpCommand extends VfsCommandBase {

    public CpCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[]   args      = parseArgs(ctx.getCommand());
        ParsedArgs parsed    = parseArgs(args);
        boolean    recursive = parsed.hasFlag("r") || parsed.hasFlag("R");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("cp: missing destination file operand");
        }

        if (parsed.positionalCount() == 2) {
            String src  = parsed.getString(0);
            String dest = parsed.getString(1);

            if (dest.endsWith("/")) {
                if (!vfs.exists(toVfsContext(ctx), dest)) {
                    vfs.createDirectory(toVfsContext(ctx), dest, true);
                }
            }

            // 如果源是目录但没有 -r，报错
            if (!recursive && vfs.isDirectory(toVfsContext(ctx), src)) {
                throw new IllegalArgumentException("cp: -r not specified; omitting directory '" + src + "'");
            }
            vfs.copy(toVfsContext(ctx), src, dest, recursive);
        } else {
            String destDir = parsed.getString(parsed.positionalCount() - 1);

            if (!vfs.exists(toVfsContext(ctx), destDir)) {
                vfs.createDirectory(toVfsContext(ctx), destDir, true);
            }

            for (int i = 0; i < parsed.positionalCount() - 1; i++) {
                String src = parsed.getString(i);
                // 如果源是目录但没有 -r，报错
                if (!recursive && vfs.isDirectory(toVfsContext(ctx), src)) {
                    throw new IllegalArgumentException("cp: -r not specified; omitting directory '" + src + "'");
                }
                vfs.copy(toVfsContext(ctx), src, destDir, recursive);
            }
        }

        return null;
    }

}
