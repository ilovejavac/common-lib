package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * write 命令 - 写入文件内容
 * 语法: write [-a] file content
 * 选项: -a 追加模式（默认覆盖）
 */
public class WriteCommand extends VfsCommandBase {

    public WriteCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[] args = parseArgs(ctx.getCommand());

        if (args.length < 2) {
            return "write: missing file operand\n";
        }

        ParsedArgs parsed = parseArgs(args);
        boolean    append = parsed.hasFlag("a");

        if (parsed.positionalCount() < 1) {
            return "write: missing file operand\n";
        }

        String        filePath = parsed.getString(0);
        StringBuilder content  = new StringBuilder();

        // 拼接所有剩余参数作为内容
        for (int i = 1; i < parsed.positionalCount(); i++) {
            if (content.length() > 0) {
                content.append(" ");
            }
            content.append(parsed.getString(i));
        }

        String fileContent = content.toString();

        if (append) {
            vfs.appendFile(toVfsContext(ctx), filePath, fileContent);
        } else {
            vfs.writeFile(toVfsContext(ctx), filePath, fileContent);
        }

        return null;
    }

}
