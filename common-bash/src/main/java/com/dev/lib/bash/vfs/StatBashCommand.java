package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsStat;

/**
 * stat 命令 - 显示文件详细信息
 */
public class StatBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("stat: missing file operand");
        }

        var vfsCtx = toVfsContext(ctx);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            VfsStat stat = Vfs.path(vfsCtx, path).stat();

            result.append("  File: ").append(stat.getPath()).append("\n");
            result.append("  Size: ").append(stat.getSize() != null ? stat.getSize() : 0).append("\n");
            result.append("  Type: ").append(Boolean.TRUE.equals(stat.getIsDirectory()) ? "directory" : "regular file").append("\n");
            result.append("Create: ").append(stat.getCreateTime() != null ? stat.getCreateTime() : "-").append("\n");
            result.append("Modify: ").append(stat.getUpdateTime() != null ? stat.getUpdateTime() : "-").append("\n");

            if (i < parsed.positionalCount() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
}
