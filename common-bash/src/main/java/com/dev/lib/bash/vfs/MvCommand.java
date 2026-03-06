package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * mv 命令
 */
public class MvCommand extends VfsCommand<Void> {

    @Override
    public Void execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("mv: missing operand");
        }

        String srcPath = parsed.getString(0);
        String destPath = parsed.getString(1);

        Vfs.move(toVfsContext(ctx), srcPath, destPath);
        return null;
    }
}
