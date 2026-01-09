package com.dev.lib.bash;

/**
 * 统一的命令执行上下文
 * 所有 Bash 命令使用此上下文
 */
public interface ExecuteContext {

    /**
     * 获取根路径
     */
    String getRoot();

    /**
     * 获取完整命令行
     */
    String getCommand();
}
