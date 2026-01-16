package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * cat 命令
 */
public class CatCommand extends VfsCommandBase {

    public CatCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[] args = parseArgs(ctx.getCommand());
        return cat(toVfsContext(ctx), args);
    }

    private Object cat(VfsContext ctx, String[] args) {

        ParsedArgs parsed          = parseArgs(args);
        boolean    showLineNumbers = parsed.hasFlag("n");
        Integer    startLine       = parsed.getInt("s", 1);
        Integer    lineCount       = parsed.getInt("c", -1);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("cat: missing file operand");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);

            if (startLine > 1 || lineCount != -1) {
                List<String> lines   = vfs.readLines(ctx, path, startLine, lineCount);
                int          lineNum = startLine;
                for (String line : lines) {
                    if (showLineNumbers) {
                        result.append(String.format("%6d\t%s\n", lineNum++, line));
                    } else {
                        result.append(line).append("\n");
                    }
                }
            } else {
                try (InputStream is = vfs.openFile(ctx, path);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String line;
                    int    lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        if (showLineNumbers) {
                            result.append(String.format("%6d\t%s\n", lineNum++, line));
                        } else {
                            result.append(line).append("\n");
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read file: " + path, e);
                }
            }
        }

        return result.toString();
    }

}
