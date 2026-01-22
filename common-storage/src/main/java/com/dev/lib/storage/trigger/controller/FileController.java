package com.dev.lib.storage.trigger.controller;

import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/sys/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

//    /**
//     * 文件上传（支持多文件、文件夹结构保留）
//     * @param files 文件数组（前端传 webkitRelativePath 作为 filename）
//     * @param category 分类（用于构建路径，如 "document" -> /document/）
//     * @return 文件 ID 列表
//     */
//    @PostMapping("/upload")
//    @OperateLog(module = "file", type = "upload", description = "上传文件", recordParams = false)
//    public ServerResponse<List<String>> upload(
//            @RequestParam("files") MultipartFile[] files,
//            @RequestParam(required = false, defaultValue = "/") String category
//    ) {
//
//        // 构建目标路径
//        String targetPath = category.startsWith("/") ? category : "/" + category;
//
//        // 从 MultipartFile.getOriginalFilename() 获取相对路径
//        // 前端使用 FormData.append('files', file, file.webkitRelativePath)
//        String[] relativePaths = new String[files.length];
//        for (int i = 0; i < files.length; i++) {
//            relativePaths[i] = files[i].getOriginalFilename();
//        }
//
//        List<String> fileIds = vfs.uploadFiles(VfsContext.of(null), targetPath, files, relativePaths);
//        return ServerResponse.success(fileIds);
//    }
//
//    /**
//     * 上传 ZIP 并解压到 VFS
//     * @return 解压后的文件 ID 列表
//     */
//    @PostMapping("/upload/zip")
//    @OperateLog(module = "file", type = "upload", description = "上传ZIP", recordParams = false)
//    public ServerResponse<List<String>> uploadZip(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(required = false, defaultValue = "/") String path
//    ) throws IOException {
//
//        List<String> fileIds = vfs.uploadZip(VfsContext.of(null), path, file.getInputStream());
//        return ServerResponse.success(fileIds);
//    }

    /**
     * 文件下载
     */
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String id,
            @RequestParam(required = false) String name
    ) throws IOException {

        StorageFile file = fileService.getById(id);
        InputStream is   = fileService.download(file);

        String filename = (name != null && !name.isBlank()) ? name : file.getOriginalName();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        filename,
                                        StandardCharsets.UTF_8
                                )
                                .build().toString()
                )
                .body(new InputStreamResource(is));
    }

    /**
     * 获取临时访问地址（7天有效）
     * 浏览器缓存响应 6 天
     */
    @GetMapping("/{id}/url")
    public ResponseEntity<String> getPresignedUrl(@PathVariable String id) {

        String url = fileService.getPresignedUrl(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(6, TimeUnit.DAYS).mustRevalidate())
                .body(url);
    }

//    /**
//     * 批量删除文件
//     * @param ids 文件 ID 列表
//     */
//    @PostMapping
//    @OperateLog(module = "file", type = "delete", description = "批量删除文件")
//    public ServerResponse<Void> delete(@RequestBody List<String> ids) {
//
//        fileService.deleteAll(ids);
//        return ServerResponse.ok();
//    }

}
