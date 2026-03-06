package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * tail 命令
 */
public class TailCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("n"), Set.of());
        int lines = parsed.getInt("n", 10);

        if (lines < 0) {
            throw new IllegalArgumentException("tail: invalid number of lines: " + lines);
        }
        if (lines == 0) {
            return "";
        }

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("tail: missing file operand");
        }

        String path = parsed.getString(0);

        try (InputStream is = Vfs.context(toVfsContext(ctx)).file(path).open();
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
