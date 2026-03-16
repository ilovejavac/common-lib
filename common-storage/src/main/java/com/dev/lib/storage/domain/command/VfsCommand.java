package com.dev.lib.storage.domain.command;

import com.dev.lib.storage.domain.model.VfsContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * VFS 命令接口 - 支持流式处理和链式组合
 * <p>
 * 设计原则：
 * - 命令间传递 InputStream，不加载到内存
 * - 支持管道组合（类似 Unix 管道）
 * - 惰性执行，只有调用 execute() 才真正执行
 */
public interface VfsCommand {

    /**
     * 执行命令
     *
     * @param ctx   VFS 上下文
     * @param input 输入流（可能为 null，表示从文件读取）
     * @return 输出流（传递给下一个命令）
     * @throws IOException 执行失败
     */
    InputStream execute(VfsContext ctx, InputStream input) throws IOException;

    /**
     * 链式组合下一个命令（管道）
     */
    default VfsCommand then(VfsCommand next) {
        return new PipeCommand(this, next);
    }

    /**
     * 是否需要输入流
     */
    default boolean requiresInput() {
        return false;
    }
}
