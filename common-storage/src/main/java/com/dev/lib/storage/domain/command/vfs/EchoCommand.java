package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * echo 命令
 */
public class EchoCommand extends VfsCommandBase {

    public EchoCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[] args = parseArgs(ctx.getCommand());

        // 标准 echo 无参数输出空行
        if (args.length == 0) {
            return "\n";
        }

        boolean       append    = false;
        boolean       noNewline = false;
        String        filePath  = null;
        StringBuilder content   = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("-n".equals(arg) && content.length() == 0 && filePath == null) {
                noNewline = true;
            } else if (">>".equals(arg)) {
                append = true;
                if (i + 1 < args.length) {
                    filePath = args[++i];
                }
            } else if (">".equals(arg)) {
                append = false;
                if (i + 1 < args.length) {
                    filePath = args[++i];
                }
            } else {
                if (content.length() > 0) {
                    content.append(" ");
                }
                content.append(arg);
            }
        }

        // Linux echo 默认添加换行符，-n 选项不添加
        String finalContent = noNewline ? content.toString() : content.toString() + "\n";

        if (filePath != null) {
            if (append) {
                vfs.appendFile(toVfsContext(ctx), filePath, finalContent);
            } else {
                vfs.writeFile(toVfsContext(ctx), filePath, finalContent);
            }
            return null;
        }

        return finalContent;
    }

}
