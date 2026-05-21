package com.dev.lib.storage.trigger.controller;

import com.dev.lib.storage.domain.service.chain.ChainStorageService;
import com.dev.lib.storage.domain.adapter.StorageFileRepo;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.util.parallel.ParallelExecutor;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/sys/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageFileRepo storageFileRepo;

    private final ChainStorageService storageService;

    /**
     * 文件下载
     */
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String id,
            @RequestParam(required = false) String name
    ) throws IOException {

        StorageFile file = storageFileRepo.findByBizId(id);
        InputStream is   = download(file);

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

        StorageFile file = storageFileRepo.findByBizId(id);
        String      url  = presignedUrl(file, 6 * 24 * 60 * 60);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(6, TimeUnit.DAYS).mustRevalidate())
                .body(url);
    }

    /**
     * 批量获取文件预签名URL（6天有效）
     */
    @PostMapping("/urls")
    public ServerResponse<Map<String, String>> getPresignedUrls(@RequestBody Collection<String> ids) {

        if (ids == null || ids.isEmpty()) {
            return ServerResponse.success(Map.of());
        }

        Map<String, String> result = new ConcurrentHashMap<>();
        ParallelExecutor.with(ids).apply(id -> {
            StorageFile file = storageFileRepo.findByBizId(id);
            if (hasStorageCoordinates(file)) {
                String url = presignedUrl(file, 6 * 24 * 60 * 60);
                result.put(id, url);
            }
        });
        return ServerResponse.success(result);
    }

    private InputStream download(StorageFile file) throws IOException {

        if (!hasStorageCoordinates(file)) {
            throw new IllegalArgumentException("file storage coordinates are missing");
        }
        return storageService.download(file.getBucketName(), file.getObjectKey());
    }

    private String presignedUrl(StorageFile file, int expireSeconds) {

        if (!hasStorageCoordinates(file)) {
            throw new IllegalArgumentException("file storage coordinates are missing");
        }
        return storageService.getPresignedUrl(file.getBucketName(), file.getObjectKey(), expireSeconds);
    }

    private boolean hasStorageCoordinates(StorageFile file) {

        return file != null
               && file.getBucketName() != null
               && !file.getBucketName().isBlank()
               && file.getObjectKey() != null
               && !file.getObjectKey().isBlank();
    }

}
