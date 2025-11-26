package com.dev.lib.storage.trigger;

import com.dev.lib.entity.log.OperateLog;
import com.dev.lib.storage.domain.service.FileService;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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
import java.io.InputStream;
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
    public ServerResponse<StorageFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "document") String category
    ) throws IOException {
        return ServerResponse.success(fileService.upload(file, category));
    }

    /**
     * 文件下载
     */
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String id,
            @RequestParam(required = false) String name
    ) throws IOException {
        StorageFile file = fileService.getById(id);
        InputStream is = fileService.download(file);

        String filename = (name != null && !name.isBlank()) ? name : file.getOriginalName();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build().toString()
                )
                .body(new InputStreamResource(is));
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