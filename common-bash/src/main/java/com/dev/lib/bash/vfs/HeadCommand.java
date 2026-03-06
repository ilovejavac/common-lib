package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.List;
import java.util.Set;

/**
 * head 命令
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
        List<String> fileLines = Vfs.readLines(toVfsContext(ctx), path, 1, lines);

        if (fileLines.isEmpty()) {
            return "";
        }
        return String.join("\n", fileLines) + "\n";
    }
}
