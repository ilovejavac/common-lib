package com.dev.lib.storage;

import com.dev.lib.entity.log.OperateLog;
import com.dev.lib.storage.data.SysFile;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/sys/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    @OperateLog(module = "file", type = "upload", description = "上传文件")
    public SysFile upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "document") String category
    ) throws IOException {
        return fileService.upload(file, category);
    }

    @Data
    public static class DownloadFileRequest {
        private String name;
    }

    /**
     * 文件下载
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(
            @PathVariable String id,
            @RequestParam DownloadFileRequest request
    ) throws IOException {
        byte[] data = fileService.download(fileService.getById(id));

        String filename = request.getName();
        if (filename == null || filename.isBlank()) {
            filename = "file";
        }
        // URL 编码处理中文文件名
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(encodedFilename, StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(data);
    }

    /**
     * 文件删除
     */
    @PostMapping("/{id}")
    @OperateLog(module = "file", type = "delete", description = "删除文件")
    public void delete(@PathVariable String id) {
        fileService.delete(fileService.getById(id));
    }
}