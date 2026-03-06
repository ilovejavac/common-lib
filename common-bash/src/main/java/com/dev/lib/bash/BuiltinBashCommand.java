package com.dev.lib.bash;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 内置 bash 命令，支持 shell 原生语法（如 &&、变量展开等）。
 *
 * 用法：
 * - bash -c "cd /tmp && echo $HOME"
 * - bash "cd /tmp && find . -name '*.txt'"
 */
public class BuiltinBashCommand extends BashCommand<Object> {

    private static final long DEFAULT_TIMEOUT_SECONDS = 300;

    @Override
    public Object execute(ExecuteContext ctx) {

        String[] tokens = parseCommandLine(ctx.getCommand());
        if (tokens.length < 2) {
            throw new IllegalArgumentException("bash: missing script");
        }

        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
        String script = resolveScript(args);

        ProcessBuilder pb = new ProcessBuilder("bash", "-lc", script);
        pb.redirectErrorStream(true);

        String root = ctx.getRoot();
        if (root != null && !root.isBlank()) {
            File workDir = new File(root);
            if (workDir.exists() && workDir.isDirectory()) {
                pb.directory(workDir);
            }
        }

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("bash: command timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("bash: exited with code " + exitCode + (output.isBlank() ? "" : "\n" + output));
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("bash: execute failed", e);
        }
    }

    private String resolveScript(String[] args) {

        if (args.length >= 2 && "-c".equals(args[0])) {
            return args[1];
        }
        return String.join(" ", args);
    }
}
