package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.Set;

/**
 * head 命令 - 显示文件前 N 行
 * 支持: -n lines
 */
public class HeadCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("n"), Set.of());
        int lines = parsed.getInt("n", 10);

        if (lines < 0) {
            throw new IllegalArgumentException("head: invalid number of lines: " + lines);
        }

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("head: missing file operand");
        }

        String path = parsed.getString(0);
        var vfsCtx = toVfsContext(ctx);

        try {
            return Vfs.path(vfsCtx, path).cat().head(lines).executeAsString();
        } catch (Exception e) {
            throw new RuntimeException("head: failed to read " + path, e);
        }
    }
}
