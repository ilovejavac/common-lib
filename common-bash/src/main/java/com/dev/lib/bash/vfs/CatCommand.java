package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * cat 命令
 */
public class CatCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        return cat(toVfsContext(ctx), args);
    }

    private String cat(VfsContext ctx, String[] args) {
        ParsedArgs parsed = parseArgs(args, Set.of("n", "s", "c"), Set.of());
        boolean showLineNumbers = parsed.hasFlag("n");
        Integer startLine = parsed.getInt("s", 1);
        Integer lineCount = parsed.getInt("c", -1);

        if (startLine == null || startLine < 1) {
            throw new IllegalArgumentException("cat: invalid start line: " + startLine);
        }
        if (lineCount == null || lineCount < -1) {
            throw new IllegalArgumentException("cat: invalid line count: " + lineCount);
        }

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("cat: missing file operand");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);

            if (startLine > 1 || lineCount != -1) {
                List<String> lines = Vfs.path(ctx, path).readLines(startLine, lineCount);
                int lineNum = startLine;
                for (String line : lines) {
                    if (showLineNumbers) {
                        result.append(String.format("%6d\t%s\n", lineNum++, line));
                    } else {
                        result.append(line).append("\n");
                    }
                }
            } else {
                try (InputStream is = Vfs.path(ctx, path).cat().execute();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String line;
                    int lineNum = 1;
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
