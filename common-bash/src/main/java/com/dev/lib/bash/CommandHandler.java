package com.dev.lib.bash;

@FunctionalInterface
public interface CommandHandler<T> {

    /**
     * 处理命令
     * @param ctx 上下文对象
     * @param args 命令参数数组
     * @return 执行结果
     */
    Object handle(T ctx, String[] args);

}
