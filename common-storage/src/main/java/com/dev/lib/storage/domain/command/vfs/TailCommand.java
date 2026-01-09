package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * tail 命令
 */
public class TailCommand extends VfsCommandBase {

    public TailCommand(VirtualFileSystem vfs) {
        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);
        int lines = parsed.getInt("n", 10);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("tail: missing file operand");
        }

        String path = parsed.getString(0);

        try (InputStream is = vfs.openFile(toVfsContext(ctx), path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String[] buffer = new String[lines];
            int index = 0;
            int count = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                buffer[index] = line;
                index = (index + 1) % lines;
                count++;
            }

            if (count == 0) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            int start = count < lines ? 0 : index;
            int end = Math.min(count, lines);

            for (int i = 0; i < end; i++) {
                result.append(buffer[(start + i) % lines]).append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }
}
