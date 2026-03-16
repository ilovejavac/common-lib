package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * cp 命令 - 复制文件/目录
 * 支持: -r/-R 递归
 */
public class CpCommand extends VfsCommand<Void> {

    @Override
    public Void execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r") || parsed.hasFlag("R");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("cp: missing operand");
        }

        String srcPath = parsed.getString(0);
        String destPath = parsed.getString(1);

        Vfs.path(toVfsContext(ctx), srcPath).cp(destPath, recursive);
        return null;
    }
}
