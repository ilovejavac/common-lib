package com.dev.lib.ai.service.agent.support.tool

import com.dev.lib.bash.BashCommandRegistry
import com.dev.lib.bash.ExecuteContext
import com.dev.lib.storage.domain.model.VfsContext
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component

/**
 * Edit 工具 - 编辑文件
 * 对应 Claude Code 的 Edit 工具
 * 委托给 sed 命令实现
 */
@Component
@RequiredArgsConstructor
class EditTool(
    val registry: BashCommandRegistry
) {

    /**
     * 替换文本（正则）
     * @param expression sed 表达式，如 "s/old/new/g" 或 "5,10s/old/new/g"
     */
    fun substitute(ctx: VfsContext, filePath: String?, expression: String) {
        executeSed(ctx, filePath, expression)
    }

    /**
     * 删除指定行
     * @param lineNumbers 行号列表，如 "5" 或 "5,10"
     */
    fun deleteLines(ctx: VfsContext, filePath: String?, lineNumbers: String?) {
        executeSed(ctx, filePath, lineNumbers + "d")
    }

    /**
     * 插入行
     * @param lineNumber 行号
     * @param content 要插入的内容
     */
    fun insertLine(ctx: VfsContext, filePath: String?, lineNumber: Int, content: String?) {
        executeSed(ctx, filePath, lineNumber.toString() + "i\\" + content)
    }

    /**
     * 追加行
     * @param lineNumber 行号
     * @param content 要追加的内容
     */
    fun appendLine(ctx: VfsContext, filePath: String?, lineNumber: Int, content: String?) {
        executeSed(ctx, filePath, lineNumber.toString() + "a\\" + content)
    }

    /**
     * 替换指定行
     * @param lineNumber 行号
     * @param content 新内容
     */
    fun changeLine(ctx: VfsContext, filePath: String?, lineNumber: Int, content: String?) {
        executeSed(ctx, filePath, lineNumber.toString() + "c\\" + content)
    }

    /**
     * 删除行号范围
     * @param start 起始行号
     * @param end 结束行号
     */
    fun deleteRange(ctx: VfsContext, filePath: String?, start: Int, end: Int) {
        executeSed(ctx, filePath, start.toString() + "," + end + "d")
    }

    /**
     * 在行号范围内替换
     * @param start 起始行号
     * @param end 结束行号
     * @param pattern 替换模式
     * @param replacement 替换内容
     * @param global 是否全局替换
     */
    fun substituteRange(
        ctx: VfsContext, filePath: String?, start: Int, end: Int,
        pattern: String?, replacement: String?, global: Boolean
    ) {
        val flag = if (global) "g" else ""
        executeSed(ctx, filePath, start.toString() + "," + end + "s/" + pattern + "/" + replacement + "/" + flag)
    }

    /**
     * 执行 sed 命令
     */
    private fun executeSed(ctx: VfsContext, filePath: String?, expression: String) {
        val command = "sed '" + expression + "' " + filePath
        val execCtx: ExecuteContext = SimpleExecuteContext(ctx.getRoot(), command)
        val registered = registry.getCommands().get("sed")
        if (registered != null) {
            registered.command.execute(execCtx)
        }
    }

    private class SimpleExecuteContext(root: String, command: String) : ExecuteContext {
        private val root: String = root
        private val command: String = command

        override fun getRoot(): String = root

        override fun getCommand(): String = command
    }

}
