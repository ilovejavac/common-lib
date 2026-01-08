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
        private String command;      // 完整命令行，如 "ls -la /path"
    }

    /**
     * 统一的 bash 命令执行接口
     * POST /sys/vfs/exec
     *
     * 示例：
     * {
     *   "root": "/workspace",
     *   "command": "ls -d 2 /path"
     * }
     *
     * {
     *   "root": "/workspace",
     *   "command": "cat -n /file.txt"
     * }
     *
     * {
     *   "root": "/workspace",
     *   "command": "rm -rf /dir1 /dir2"
     * }
     *
     * {
     *   "root": "/workspace",
     *   "command": "cat 'file with spaces.txt'"
     * }
     */
    @PostMapping("/exec")
    public ServerResponse<Object> exec(@RequestBody BashRequest req) {
        VfsContext ctx = VfsContext.of(req.getRoot());
        Object result = bashCommand.execute(ctx, req.getCommand());
        return ServerResponse.success(result);
    }

}
