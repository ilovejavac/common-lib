package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.BashCommand;
import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.util.Arrays;

/**
 * VFS 命令基类
 */
public abstract class VfsCommandBase<T extends Object> extends BashCommand<T> {

    protected final VirtualFileSystem vfs;

    protected VfsCommandBase(VirtualFileSystem vfs) {

        this.vfs = vfs;
    }

    protected VfsContext toVfsContext(ExecuteContext ctx) {

        return VfsContext.of(ctx.getRoot());
    }

    protected String[] parseArgs(String commandLine) {

        String[] tokens = parseCommandLine(commandLine);
        return tokens.length > 1
               ? Arrays.copyOfRange(tokens, 1, tokens.length)
               : new String[0];
    }

}
