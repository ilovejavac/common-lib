package com.dev.lib.storage.domain.service.virtual.search;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.node.VfsNodeFactory;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS 查找服务
 * 负责按名称和内容查找文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsSearchService {

    private final VfsFileRepository     fileRepository;
    private final VfsPathResolver       pathResolver;
    private final VfsNodeFactory        nodeFactory;
    private final VfsFileStorageService storageService;

    // ==================== 按名称查找 ====================

    /**
     * 按名称模式查找文件
     *
     * @param ctx      VFS 上下文
     * @param basePath 基础路径
     * @param pattern  通配符模式（支持 * 和 ?）
     * @param recursive 是否递归查找
     * @return 匹配的节点列表
     */
    public List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {

        String fullBasePath = pathResolver.resolve(ctx, basePath);

        SysFile base = fileRepository.findByPath(fullBasePath)
                .orElseThrow(() -> new IllegalArgumentException("Base path not found: " + basePath));

        if (!Boolean.TRUE.equals(base.getIsDirectory())) {
            throw new IllegalArgumentException("Base path is not a directory: " + basePath);
        }

        List<VfsNode> results  = new ArrayList<>();
        String        baseName = pathResolver.getName(fullBasePath);

        if (baseName != null && VfsGlobMatcher.matches(pattern, baseName)) {
            results.add(nodeFactory.toNode(ctx, base, 0));
        }

        searchRecursiveByName(ctx, fullBasePath, pattern, recursive, results);
        return results;
    }

    private void searchRecursiveByName(VfsContext ctx, String currentPath, String pattern,
                                       boolean recursive, List<VfsNode> results) {

        List<SysFile> children = fileRepository.findChildren(currentPath);
        for (SysFile child : children) {
            String name = pathResolver.getName(child.getVirtualPath());
            if (VfsGlobMatcher.matches(pattern, name)) {
                results.add(nodeFactory.toNode(ctx, child, 0));
            }
            if (recursive && Boolean.TRUE.equals(child.getIsDirectory())) {
                searchRecursiveByName(ctx, child.getVirtualPath(), pattern, true, results);
            }
        }
    }

    // ==================== 按内容查找 ====================

    /**
     * 按文件内容查找
     *
     * @param ctx         VFS 上下文
     * @param basePath    基础路径
     * @param content     要查找的内容
     * @param recursive   是否递归查找
     * @return 包含该内容的节点列表
     */
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {

        return findByContent(ctx, basePath, content, recursive, false);
    }

    /**
     * 按文件内容查找（带错误控制）
     *
     * @param ctx           VFS 上下文
     * @param basePath      基础路径
     * @param content       要查找的内容
     * @param recursive     是否递归查找
     * @param strictMode    严格模式：true 时遇到错误抛出异常，false 时跳过错误文件
     * @return 包含该内容的节点列表
     * @throws RuntimeException 如果 strictMode 为 true 且搜索失败
     */
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content,
                                       boolean recursive, boolean strictMode) {

        String fullBasePath = pathResolver.resolve(ctx, basePath);

        SysFile base = fileRepository.findByPath(fullBasePath)
                .orElseThrow(() -> new IllegalArgumentException("Base path not found: " + basePath));

        if (!Boolean.TRUE.equals(base.getIsDirectory())) {
            throw new IllegalArgumentException("Base path is not a directory: " + basePath);
        }

        List<VfsNode> results = new ArrayList<>();
        SearchStats  stats  = new SearchStats();
        searchRecursiveByContent(ctx, fullBasePath, content, recursive, results, strictMode, stats);

        if (stats.errorCount > 0) {
            log.warn("Search completed with {} errors out of {} files", stats.errorCount, stats.totalFiles);
        }

        return results;
    }

    private void searchRecursiveByContent(VfsContext ctx, String currentPath, String searchContent,
                                          boolean recursive, List<VfsNode> results, boolean strictMode, SearchStats stats) {

        List<SysFile> children = fileRepository.findChildren(currentPath);
        for (SysFile child : children) {
            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                if (recursive) {
                    searchRecursiveByContent(ctx, child.getVirtualPath(), searchContent, true, results, strictMode, stats);
                }
            } else {
                stats.totalFiles++;
                if (fileContainsContent(child, searchContent, strictMode, stats)) {
                    results.add(nodeFactory.toNode(ctx, child, 0));
                }
            }
        }
    }

    private boolean fileContainsContent(SysFile file, String searchContent, boolean strictMode, SearchStats stats) {

        try (InputStream is = storageService.download(file.getStoragePath());
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(searchContent)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            stats.errorCount++;
            if (strictMode) {
                throw new RuntimeException("Failed to search content in file: " + file.getVirtualPath(), e);
            }
            log.warn("Failed to search content in file: {} - {}", file.getVirtualPath(), e.getMessage());
            return false;
        }
    }

    /**
     * 搜索统计信息（内部使用）
     */
    private static class SearchStats {
        int totalFiles = 0;
        int errorCount = 0;
    }
}
