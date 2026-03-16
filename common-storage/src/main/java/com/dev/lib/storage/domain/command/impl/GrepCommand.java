package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Grep 命令 - 内容过滤（流式处理）
 */
@RequiredArgsConstructor
public class GrepCommand implements VfsCommand {

    private final String pattern;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Grep requires input stream");
        }

        // 流式过滤，逐行处理
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(pattern)) {
                    writer.write(line);
                    writer.newLine();
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
