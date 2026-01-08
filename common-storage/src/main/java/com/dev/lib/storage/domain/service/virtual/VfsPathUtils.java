package com.dev.lib.storage.domain.service.virtual;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

/**
 * VFS 路径工具类
 */
public final class VfsPathUtils {

    private VfsPathUtils() {}

    /**
     * 解析路径，合并 root 和 path
     */
    public static String resolvePath(String root, String path) {

        if (root == null) root = "";
        if (path == null) path = "";

        root = normalizePath(root);
        path = normalizePath(path);

        if (path.isEmpty()) {
            return root.isEmpty() ? "/" : root;
        }
        if (root.isEmpty()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        return root + (path.startsWith("/") ? path : "/" + path);
    }

    /**
     * 规范化路径，处理 . 和 .. 以及多余的 /
     */
    public static String normalizePath(String path) {

        if (path == null || path.isEmpty()) return "";

        String[]      parts = path.split("/");
        Deque<String> stack = new ArrayDeque<>();

        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (!stack.isEmpty()) stack.pollLast();
            } else {
                stack.addLast(part);
            }
        }
        return stack.isEmpty() ? "/" : "/" + String.join("/", stack);
    }

    /**
     * 获取父路径
     */
    public static String getParentPath(String virtualPath) {

        if (virtualPath == null || "/".equals(virtualPath)) return null;
        int idx = virtualPath.lastIndexOf('/');
        return idx <= 0 ? "/" : virtualPath.substring(0, idx);
    }

    /**
     * 获取文件/目录名
     */
    public static String getName(String virtualPath) {

        if (virtualPath == null) return null;
        int idx = virtualPath.lastIndexOf('/');
        return idx < 0 ? virtualPath : virtualPath.substring(idx + 1);
    }

    /**
     * 检查 child 是否是 parent 的子路径（或相同）
     */
    public static boolean isSubPath(String parent, String child) {

        if (parent == null || child == null) return false;
        if (parent.equals(child)) return true;
        String prefix = parent.endsWith("/") ? parent : parent + "/";
        return child.startsWith(prefix);
    }

    /**
     * 通配符模式匹配（支持 * 和 ?）
     */
    public static boolean matchPattern(String name, String pattern) {

        if (pattern == null || pattern.isEmpty()) return true;

        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '.':
                    regex.append("\\.");
                    break;
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '^':
                case '$':
                case '+':
                case '|':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
            }
        }

        return Pattern.matches(regex.toString(), name);
    }

    /**
     * 获取文件扩展名（小写）
     */
    public static String getExtension(String fileName) {

        if (fileName == null) return "";
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(dotIdx + 1).toLowerCase() : "";
    }
}
