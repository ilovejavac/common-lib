package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;

/**
 * pwd 命令 - 显示当前路径
 */
public class PwdBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String root = ctx.getRoot();
        return (root == null || root.isBlank()) ? "/" : root;
    }
}
