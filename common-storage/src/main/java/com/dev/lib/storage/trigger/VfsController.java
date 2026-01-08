package com.dev.lib.storage.trigger;

import com.dev.lib.storage.domain.command.VfsBashCommand;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import com.dev.lib.web.model.ServerResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/sys/vfs")
@RequiredArgsConstructor
public class VfsController {

    private final VirtualFileSystem vfs;
    private final VfsBashCommand bashCommand;

    @Data
    public static class BashRequest {
        private String root;         // 根路径
        private String command;      // 命令名（如 "ls", "cat", "rm"）
        private Object[] args;       // 命令参数
    }

    /**
     * 统一的 bash 命令执行接口
     * POST /sys/vfs/exec
     *
     * 示例：
     * {
     *   "root": "/workspace",
     *   "command": "ls",
     *   "args": ["-d", "2", "/path"]
     * }
     *
     * {
     *   "root": "/workspace",
     *   "command": "cat",
     *   "args": ["-n", "/file.txt"]
     * }
     *
     * {
     *   "root": "/workspace",
     *   "command": "rm",
     *   "args": ["-rf", "/dir1", "/dir2"]
     * }
     */
    @PostMapping("/exec")
    public ServerResponse<Object> exec(@RequestBody BashRequest req) {
        VfsContext ctx = VfsContext.of(req.getRoot());

        // 构造完整参数：ctx + command + args
        Object[] fullArgs = new Object[req.getArgs().length + 2];
        fullArgs[0] = ctx;
        fullArgs[1] = req.getCommand();
        System.arraycopy(req.getArgs(), 0, fullArgs, 2, req.getArgs().length);

        Object result = bashCommand.execute(fullArgs);
        return ServerResponse.success(result);
    }

    /**
     * 上传 ZIP 文件并解压
     * POST /sys/vfs/upload-zip
     */
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

