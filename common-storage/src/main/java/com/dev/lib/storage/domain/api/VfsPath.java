package com.dev.lib.storage.domain.api;

import com.dev.lib.storage.domain.command.VfsCommand;
import com.dev.lib.storage.domain.command.impl.*;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.core.VfsCoreDirectoryService;
import com.dev.lib.storage.domain.service.virtual.core.VfsFileService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * VFS 路径 API - 提供 Linux 风格的链式操作
 * <p>
 * 使用示例：
 * <pre>
 * // 读取文件并过滤
 * String result = Vfs.path("/logs/app.log")
 *     .cat()
 *     .grep("ERROR")
 *     .head(10)
 *     .executeAsString();
 *
 * // 文件操作
 * Vfs.path("/data/file.txt").touch();
 * Vfs.path("/src").cp("/backup", true);
 *
 * // 目录列表
 * List<VfsNode> files = Vfs.path("/logs/*.log").ls();
 * </pre>
 */
@RequiredArgsConstructor
public class VfsPath {

    private final VfsContext ctx;
    private final String path;
    private final VfsFileService fileService;
    private final VfsCoreDirectoryService directoryService;

    private VfsCommand commandChain;

    // ========== 文件读取（流式） ==========

    /**
     * 读取文件内容
     */
    public VfsPath cat() {
        appendCommand(new CatCommand(fileService, path));
        return this;
    }

    /**
     * 读取前 N 行
     */
    public VfsPath head(int lines) {
        appendCommand(new HeadCommand(lines));
        return this;
    }

    /**
     * 读取后 N 行
     */
    public VfsPath tail(int lines) {
        appendCommand(new TailCommand(lines));
        return this;
    }

    // ========== 文本处理（流式） ==========

    /**
     * 内容过滤
     */
    public VfsPath grep(String pattern) {
        appendCommand(new GrepCommand(pattern));
        return this;
    }

    /**
     * 流式替换
     */
    public VfsPath sed(String pattern, String replacement) {
        appendCommand(new SedCommand(pattern, replacement));
        return this;
    }

    // ========== 统计 ==========

    /**
     * 统计行数/字数/字节数
     */
    public WcCommand.WcResult wc() {
        appendCommand(new WcCommand());
        try (InputStream result = execute()) {
            return WcCommand.parseResult(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute wc", e);
        }
    }

    // ========== 写入（流式） ==========

    /**
     * 写入到目标文件
     */
    public VfsPath writeTo(String targetPath) {
        appendCommand(new WriteCommand(fileService, targetPath));
        return this;
    }

    /**
     * 写入字符串内容
     */
    public void write(String content) {
        fileService.write(ctx, path, content);
    }

    /**
     * 写入输入流
     */
    public void write(InputStream input) {
        fileService.writeStream(ctx, path, input);
    }

    /**
     * 追加内容
     */
    public void append(String content) {
        fileService.append(ctx, path, content);
    }

    // ========== 文件操作 ==========

    /**
     * 创建空文件或更新时间戳
     */
    public void touch() {
        fileService.touch(ctx, path);
    }

    /**
     * 复制文件/目录
     */
    public void cp(String dest, boolean recursive) {
        directoryService.copy(ctx, path, dest, recursive);
    }

    /**
     * 移动/重命名
     */
    public void mv(String dest) {
        directoryService.move(ctx, path, dest);
    }

    /**
     * 删除文件/目录
     */
    public void rm(boolean recursive) {
        directoryService.delete(ctx, path, recursive);
    }

    // ========== 目录操作 ==========

    /**
     * 列出目录内容（支持通配符）
     */
    public List<VfsNode> ls() {
        return ls(false);
    }

    /**
     * 列出目录内容
     */
    public List<VfsNode> ls(boolean showHidden) {
        return directoryService.list(ctx, path, showHidden);
    }

    /**
     * 创建目录
     */
    public void mkdir(boolean parents) {
        if (parents) {
            directoryService.mkdirp(ctx, path);
        } else {
            directoryService.mkdir(ctx, path);
        }
    }

    /**
     * 树形显示目录结构
     */
    public String tree(int depth) {
        return directoryService.tree(ctx, path, depth);
    }

    // ========== 搜索 ==========

    /**
     * 按名称模式搜索（支持通配符）
     */
    public List<VfsNode> find(String pattern) {
        return directoryService.findByPattern(ctx, path, pattern);
    }

    /**
     * 按内容搜索
     */
    public List<VfsNode> findByContent(String content) {
        return directoryService.findByContent(ctx, path, content);
    }

    // ========== 文件信息 ==========

    /**
     * 检查文件是否存在
     */
    public boolean exists() {
        return fileService.exists(ctx, path);
    }

    /**
     * 检查是否是目录
     */
    public boolean isDirectory() {
        return directoryService.isDirectory(ctx, path);
    }

    /**
     * 获取文件大小
     */
    public long size() {
        return fileService.getSize(ctx, path);
    }

    /**
     * 获取行数
     */
    public int lineCount() {
        return fileService.getLineCount(ctx, path);
    }

    // ========== 执行命令链 ==========

    /**
     * 执行命令链，返回输入流
     */
    public InputStream execute() throws IOException {
        if (commandChain == null) {
            throw new IllegalStateException("No commands to execute");
        }
        return commandChain.execute(ctx, null);
    }

    /**
     * 执行命令链，返回字符串
     */
    public String executeAsString() throws IOException {
        try (InputStream result = execute()) {
            if (result == null) {
                return "";
            }
            return new String(result.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ========== 私有辅助方法 ==========

    private void appendCommand(VfsCommand cmd) {
        if (commandChain == null) {
            commandChain = cmd;
        } else {
            commandChain = commandChain.then(cmd);
        }
    }
}
