package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Uniq 命令 - 去除连续重复行
 * <p>
 * 等价于 Linux: uniq
 * 通常配合 sort 使用: sort | uniq
 */
public class UniqCommand implements VfsCommand {

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Uniq requires input stream");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            String prev = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals(prev)) {
                    writer.write(line);
                    writer.newLine();
                    prev = line;
                }
            }
            writer.flush();
        }

        return new ByteArrayInputStream(output.toByteArray());
    }

    @Override
    public boolean requiresInput() {
        return true;
    }
}
