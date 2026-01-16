package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.util.List;

/**
 * head 命令
 */
public class HeadCommand extends VfsCommandBase {

    public HeadCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[]   args   = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);
        int        lines  = parsed.getInt("n", 10);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("head: missing file operand");
        }

        String       path      = parsed.getString(0);
        List<String> fileLines = vfs.readLines(toVfsContext(ctx), path, 1, lines);

        if (fileLines.isEmpty()) {
            return "";
        }
        return String.join("\n", fileLines) + "\n";
    }

}
