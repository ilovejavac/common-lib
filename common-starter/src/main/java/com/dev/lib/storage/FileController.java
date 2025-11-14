package com.dev.lib.storage;

import com.dev.lib.entity.log.OperateLog;
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
    @OperateLog(module = "文件管理", type = "上传", description = "上传文件")
    public SysFile upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "document") String category
    ) throws IOException {
        return fileService.upload(file, category);
    }

    /**
     * 文件下载
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
        byte[] data = fileService.download(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("file").build());

        return ResponseEntity.ok().headers(headers).body(data);
    }

    /**
     * 文件删除
     */
    @PostMapping("/{id}")
    @OperateLog(module = "文件管理", type = "删除", description = "删除文件")
    public void delete(@PathVariable Long id) {
        fileService.delete(id);
    }
}