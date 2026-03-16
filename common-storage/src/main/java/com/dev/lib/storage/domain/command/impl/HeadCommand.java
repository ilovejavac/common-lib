package com.dev.lib.storage.domain.command.impl;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Head 命令 - 读取前 N 行
 */
@RequiredArgsConstructor
public class HeadCommand implements VfsCommand {

    private final int lines;

    @Override
    public InputStream execute(VfsContext ctx, InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Head requires input stream");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < lines) {
                writer.write(line);
                writer.newLine();
                count++;
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
