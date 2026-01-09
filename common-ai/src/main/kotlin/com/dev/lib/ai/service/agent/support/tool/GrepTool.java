package com.dev.lib.ai.service.agent.support.tool;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Grep 工具 - 搜索文件内容
 * 对应 Claude Code 的 Grep/Search 工具
 */
@Component
@RequiredArgsConstructor
public class GrepTool {

    private final VirtualFileSystem vfs;

    /**
     * 按文件名搜索
     * @param pattern 文件名模式（支持通配符 * 和 ?）
     * @param recursive 是否递归搜索
     */
    public List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {
        return vfs.findByName(ctx, basePath, pattern, recursive);
    }

    /**
     * 按内容搜索（简单字符串匹配）
     * @param content 要搜索的内容
     * @param recursive 是否递归搜索
     */
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {
        return vfs.findByContent(ctx, basePath, content, recursive);
    }

    /**
     * 按正则表达式搜索内容
     * @param regex 正则表达式
     * @param recursive 是否递归搜索
     * @param ignoreCase 是否忽略大小写
     */
    public List<GrepResult> grepRegex(VfsContext ctx, String basePath, String regex,
                                       boolean recursive, boolean ignoreCase) {
        List<VfsNode> matchedFiles = vfs.findByContent(ctx, basePath, extractLiteralPattern(regex), recursive);
        Pattern pattern = ignoreCase
                ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                : Pattern.compile(regex);

        return grepFiles(ctx, matchedFiles, pattern);
    }

    /**
     * 从正则表达式中提取字面量模式（用于 findByContent 预筛选）
     */
    private String extractLiteralPattern(String regex) {
        // 简单提取：移除正则元字符，保留字母数字
        StringBuilder sb = new StringBuilder();
        for (char c : regex.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.length() > 0 ? sb.toString() : regex;
    }

    /**
     * 对文件列表执行正则匹配
     */
    private List<GrepResult> grepFiles(VfsContext ctx, List<VfsNode> files, Pattern pattern) {
        return files.stream()
                .map(node -> grepFile(ctx, node.getPath(), pattern))
                .filter(GrepResult::hasMatches)
                .toList();
    }

    /**
     * 对单个文件执行正则匹配
     */
    private GrepResult grepFile(VfsContext ctx, String path, Pattern pattern) {
        try (var is = vfs.openFile(ctx, path);
             var reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {

            List<GrepMatch> matches = new java.util.ArrayList<>();
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (pattern.matcher(line).find()) {
                    matches.add(new GrepMatch(lineNum, line));
                }
            }
            return new GrepResult(path, matches);
        } catch (Exception e) {
            return new GrepResult(path, java.util.List.of());
        }
    }

    /**
     * Grep 匹配结果
     */
    public record GrepResult(String path, List<GrepMatch> matches) {
        public boolean hasMatches() {
            return !matches.isEmpty();
        }
    }

    /**
     * 单行匹配结果
     */
    public record GrepMatch(int lineNumber, String content) {}
}
