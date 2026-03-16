package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Sed 命令 - 流式替换
 */
@RequiredArgsConstructor
public class SedCommand implements VfsCommand {

    private final String pattern;
    private final String replacement;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Sed requires input stream");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String replaced = line.replace(pattern, replacement);
                writer.write(replaced);
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
