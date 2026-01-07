package com.dev.lib.storage.trigger;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import com.dev.lib.web.model.ServerResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/sys/vfs")
@RequiredArgsConstructor
public class VfsController {

    private final VirtualFileSystem vfs;

    @Data
    public static class VfsRequest {

        private Integer n;

        private String  root;

    }

    @Data
    public static class WriteRequest extends VfsRequest {

        private String path;

        private String content;

    }

    @Data
    public static class MoveRequest extends VfsRequest {

        private String src;

        private String dest;

    }

    @PostMapping("/ls")
    public ServerResponse<List<VfsNode>> ls(
            @RequestBody VfsRequest req,
            @RequestParam(required = false) String path,
            @RequestParam(defaultValue = "1") Integer depth) {

        return ServerResponse.success(vfs.ls(VfsContext.of(req.getRoot()), path, depth));
    }

    @PostMapping("/cat")
    public ResponseEntity<InputStreamResource> cat(
            @RequestBody VfsRequest req,
            @RequestParam String path) {

        InputStream is = vfs.cat(VfsContext.of(req.getRoot()), path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(is));
    }

    @PostMapping("/read")
    public ServerResponse<String> read(
            @RequestBody VfsRequest req,
            @RequestParam String path) {

        return ServerResponse.success(vfs.read(VfsContext.of(req.getRoot()), path));
    }

    @PostMapping("/write")
    public ServerResponse<Void> write(@RequestBody WriteRequest req) {

        vfs.write(VfsContext.of(req.getRoot()), req.getPath(), req.getContent());
        return ServerResponse.ok();
    }

    @PostMapping("/mv")
    public ServerResponse<Void> mv(@RequestBody MoveRequest req) {

        vfs.mv(VfsContext.of(req.getRoot()), req.getSrc(), req.getDest());
        return ServerResponse.ok();
    }

    @PostMapping("/cp")
    public ServerResponse<Void> cp(@RequestBody MoveRequest req) {

        vfs.cp(VfsContext.of(req.getRoot()), req.getSrc(), req.getDest());
        return ServerResponse.ok();
    }

    @PostMapping("/rm")
    public ServerResponse<Void> rm(
            @RequestBody VfsRequest req,
            @RequestParam String path,
            @RequestParam(defaultValue = "false") Boolean recursive) {

        if (recursive) {
            vfs.rmrf(VfsContext.of(req.getRoot()), path);
        } else {
            vfs.rm(VfsContext.of(req.getRoot()), path);
        }

        return ServerResponse.ok();
    }

    @PostMapping("/mkdir")
    public ServerResponse<Void> mkdir(
            @RequestBody VfsRequest req,
            @RequestParam String path,
            @RequestParam(defaultValue = "false") Boolean parents) {

        if (parents) {
            vfs.mkdirp(VfsContext.of(req.getRoot()), path);
        } else {
            vfs.mkdir(VfsContext.of(req.getRoot()), path);
        }

        return ServerResponse.ok();
    }

    @PostMapping("/upload-zip")
    public ServerResponse<Void> uploadZip(
            @RequestParam String tenantId,
            @RequestParam(required = false) String root,
            @RequestParam(required = false, defaultValue = "/") String path,
            @RequestParam("file") MultipartFile file) throws Exception {

        vfs.uploadZip(VfsContext.of(root), path, file.getInputStream());
        return ServerResponse.ok();
    }

}
