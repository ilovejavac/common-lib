package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsNode;

import java.util.ArrayList;
import java.util.List;

/**
 * find 命令
 * 语法: find path -name pattern [-maxdepth n] [-type f|d]
 */
public class FindCommand extends VfsCommand<List<VfsNode>> {

    @Override
    public List<VfsNode> execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        String basePath = ".";
        String pattern = "*";
        Integer maxDepth = null;
        String type = null;

        int i = 0;
        if (args.length > 0 && !args[0].startsWith("-")) {
            basePath = args[0];
            i = 1;
        }

        for (; i < args.length; i++) {
            switch (args[i]) {
                case "-name" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("find: missing argument to `-name`");
                    }
                    pattern = args[++i];
                }
                case "-maxdepth" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("find: missing argument to `-maxdepth`");
                    }
                    maxDepth = Integer.parseInt(args[++i]);
                    if (maxDepth < 0) {
                        throw new IllegalArgumentException("find: invalid -maxdepth: " + maxDepth);
                    }
                }
                case "-type" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("find: missing argument to `-type`");
                    }
                    type = args[++i];
                    if (!"f".equals(type) && !"d".equals(type)) {
                        throw new IllegalArgumentException("find: unsupported type: " + type);
                    }
                }
                default -> throw new IllegalArgumentException("find: unknown predicate: " + args[i]);
            }
        }

        var vfsCtx = toVfsContext(ctx);
        String resolvedBasePath = resolveBasePath(vfsCtx, basePath);
        List<VfsNode> nodes = Vfs.findByName(vfsCtx, basePath, pattern, true);
        List<VfsNode> results = new ArrayList<>();

        for (VfsNode node : nodes) {
            if (maxDepth != null && relativeDepth(resolvedBasePath, node.getPath()) > maxDepth) {
                continue;
            }
            if ("f".equals(type) && Boolean.TRUE.equals(node.getIsDirectory())) {
                continue;
            }
            if ("d".equals(type) && !Boolean.TRUE.equals(node.getIsDirectory())) {
                continue;
            }
            results.add(node);
        }

        return results;
    }

    private int relativeDepth(String basePath, String targetPath) {
        String normalizedBase = normalize(basePath);
        String normalizedTarget = normalize(targetPath);
        if (normalizedBase.equals(normalizedTarget)) {
            return 0;
        }
        String prefix = "/".equals(normalizedBase) ? "/" : normalizedBase + "/";
        if (!normalizedTarget.startsWith(prefix)) {
            return Integer.MAX_VALUE;
        }
        String rest = normalizedTarget.substring(prefix.length());
        if (rest.isBlank()) {
            return 0;
        }
        int depth = 1;
        for (int i = 0; i < rest.length(); i++) {
            if (rest.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }

    private String normalize(String path) {
        if (path == null || path.isBlank() || ".".equals(path)) {
            return "/";
        }
        String normalized = path;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String resolveBasePath(com.dev.lib.storage.domain.model.VfsContext ctx, String basePath) {
        List<VfsNode> nodes = Vfs.listDirectory(ctx, basePath, 0);
        if (nodes == null || nodes.isEmpty() || nodes.get(0).getPath() == null) {
            return normalize(basePath);
        }
        return normalize(nodes.get(0).getPath());
    }
}
