package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 虚拟文件系统底层接口
 * 只提供基础的文件操作能力，不关心命令行参数解析
 */
public interface VirtualFileSystem {

    // ==================== 基础查询 ====================

    /**
     * 列出目录内容
     */
    List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth);

    /**
     * 打开文件流（用于流式读取，避免 OOM）
     */
    InputStream openFile(VfsContext ctx, String path);

    /**
     * 读取文件全部内容到内存（小文件使用）
     */
    String readFile(VfsContext ctx, String path);

    /**
     * 读取文件指定行范围（避免 OOM）
     * @param startLine 起始行号（从 1 开始）
     * @param lineCount 读取行数（-1 表示读到文件末尾）
     * @return 每行内容的列表
     */
    List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount);

    /**
     * 获取文件总行数
     */
    int getLineCount(VfsContext ctx, String path);

    /**
     * 检查路径是否存在
     */
    boolean exists(VfsContext ctx, String path);

    /**
     * 检查是否为目录
     */
    boolean isDirectory(VfsContext ctx, String path);

    // ==================== 文件操作 ====================

    /**
     * 写入文件（字符串）
     */
    void writeFile(VfsContext ctx, String path, String content);

    /**
     * 写入文件（流）
     */
    void writeFile(VfsContext ctx, String path, InputStream inputStream);

    /**
     * 创建空文件或更新时间戳
     */
    void touchFile(VfsContext ctx, String path);

    /**
     * 复制文件或目录
     * @param recursive 是否递归复制目录
     */
    void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive);

    /**
     * 移动/重命名文件或目录
     */
    void move(VfsContext ctx, String srcPath, String destPath);

    /**
     * 删除文件或目录
     * @param recursive 是否递归删除
     */
    void delete(VfsContext ctx, String path, boolean recursive);

    // ==================== 目录操作 ====================

    /**
     * 创建目录
     * @param createParents 是否创建父目录
     */
    void createDirectory(VfsContext ctx, String path, boolean createParents);

    // ==================== 搜索功能 ====================

    /**
     * 按文件名模式搜索
     * @param pattern 文件名模式（支持通配符 * 和 ?）
     * @param recursive 是否递归搜索
     */
    List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive);

    /**
     * 搜索文件内容
     * @param content 要搜索的内容
     * @param recursive 是否递归搜索
     */
    List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive);

    // ==================== 高级功能 ====================

    /**
     * 上传并解压 ZIP 文件
     * @return 创建的文件 ID 列表
     */
    List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream);

    /**
     * 批量上传文件到虚拟文件系统
     * @param ctx 虚拟文件系统上下文
     * @param targetPath 目标路径
     * @param files 文件数组
     * @param relativePaths 相对路径数组（对应每个文件的相对路径，如 "folder/sub/file.txt"）
     * @return 创建的文件 ID 列表
     */
    List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths);

}
