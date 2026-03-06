package com.dev.lib.bash.vfs;

import com.dev.lib.bash.BashCommand;
import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.model.VfsContext;

import java.util.Arrays;

/**
 * VFS 命令基类
 * 通过 Vfs API 操作虚拟文件系统
 */
public abstract class VfsCommand<T> extends BashCommand<T> {

    protected VfsContext toVfsContext(ExecuteContext ctx) {
        return VfsContext.of(ctx.getRoot());
    }

    protected String[] parseArgs(String commandLine) {
        String[] tokens = parseCommandLine(commandLine);
        return tokens.length > 1
               ? Arrays.copyOfRange(tokens, 1, tokens.length)
               : new String[0];
    }

    protected String baseName(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        String normalized = path;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }
}
