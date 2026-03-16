package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tail 命令 - 读取后 N 行
 */
@RequiredArgsConstructor
public class TailCommand implements VfsCommand {

    private final int lines;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Tail requires input stream");
        }

        // 使用循环队列保存最后 N 行
        Deque<String> buffer = new ArrayDeque<>(lines);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (buffer.size() >= lines) {
                    buffer.pollFirst();
                }
                buffer.addLast(line);
            }
        }

        // 输出最后 N 行
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            for (String line : buffer) {
                writer.write(line);
                writer.newLine();
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
