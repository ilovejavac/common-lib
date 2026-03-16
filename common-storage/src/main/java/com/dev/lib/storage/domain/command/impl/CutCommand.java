package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Cut 命令 - 按分隔符提取列
 * <p>
 * 等价于 Linux: cut -d',' -f1,3
 */
@RequiredArgsConstructor
public class CutCommand implements VfsCommand {

    private final String delimiter;
    private final int[] fields;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Cut requires input stream");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(java.util.regex.Pattern.quote(delimiter), -1);
                String result = Arrays.stream(fields)
                        .filter(f -> f > 0 && f <= parts.length)
                        .mapToObj(f -> parts[f - 1])
                        .collect(Collectors.joining(delimiter));
                writer.write(result);
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
