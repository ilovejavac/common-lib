package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.command.impl.WcCommand.WcResult;

import java.util.Set;

/**
 * wc 命令 - 统计行数/字数/字节数
 * 支持: -l 只显示行数, -w 只显示字数, -c 只显示字节数
 */
public class WcBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("l", "w", "c"), Set.of());

        boolean linesOnly = parsed.hasFlag("l");
        boolean wordsOnly = parsed.hasFlag("w");
        boolean bytesOnly = parsed.hasFlag("c");
        boolean showAll = !linesOnly && !wordsOnly && !bytesOnly;

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("wc: missing file operand");
        }

        StringBuilder result = new StringBuilder();
        var vfsCtx = toVfsContext(ctx);

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            WcResult wc = Vfs.path(vfsCtx, path).cat().wc();

            StringBuilder line = new StringBuilder();
            if (showAll || linesOnly) line.append(String.format("%8d", wc.lines()));
            if (showAll || wordsOnly) line.append(String.format("%8d", wc.words()));
            if (showAll || bytesOnly) line.append(String.format("%8d", wc.bytes()));
            line.append(" ").append(path);
            result.append(line).append("\n");
        }

        return result.toString();
    }
}
