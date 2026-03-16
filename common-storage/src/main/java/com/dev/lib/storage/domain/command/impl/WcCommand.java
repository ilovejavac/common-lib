package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Wc 命令 - 统计行数/字数/字节数
 */
public class WcCommand implements VfsCommand {

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Wc requires input stream");
        }

        long lines = 0;
        long words = 0;
        long bytes = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                bytes += line.getBytes(StandardCharsets.UTF_8).length + 1; // +1 for newline
                words += line.split("\\s+").length;
            }
        }

        String result = String.format("%d %d %d", lines, words, bytes);
        return new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean requiresInput() {
        return true;
    }

    /**
     * 解析 wc 结果
     */
    public static WcResult parseResult(InputStream input) throws IOException {
        String result = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        String[] parts = result.split("\\s+");
        return new WcResult(
            Long.parseLong(parts[0]),
            Long.parseLong(parts[1]),
            Long.parseLong(parts[2])
        );
    }

    /**
     * Wc 结果
     */
    public record WcResult(long lines, long words, long bytes) {}
}
