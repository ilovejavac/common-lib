package com.dev.lib.bash.controller;

import com.dev.lib.bash.BashCommand;
import com.dev.lib.bash.BashCommandRegistry;
import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.model.ServerResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用 Bash 命令执行控制器
 * 各模块注册命令后通过此接口统一执行
 */
@RestController
@RequestMapping("/sys/bash")
@RequiredArgsConstructor
public class BashController {

    private final BashCommandRegistry registry;

    @Data
    public static class ExecuteRequest {

        private String root;         // 根路径

        private String command;      // 完整命令行，如 "ls -la /path"

    }

    /**
     * 统一的命令执行接口
     * POST /sys/bash/exec
     * <p>
     * 示例：
     * {
     * "root": "/workspace",
     * "command": "ls -d 2 /path"
     * }
     * <p>
     * {
     * "root": "/workspace",
     * "command": "cat -n /file.txt"
     * }
     */
    @PostMapping("/exec")
    public ServerResponse<Object> exec(@RequestBody ExecuteRequest req) {

        if (req.getCommand() == null || req.getCommand().isBlank()) {
            throw new BizException(400001, "Command cannot be empty");
        }

        // 解析命令行获取命令名称
        String[] tokens  = req.getCommand().trim().split("\\s+", 2);
        String   cmdName = tokens[0];

        // 查找注册的命令
        BashCommandRegistry.RegisteredCommand registered = registry.getCommands().get(cmdName);
        if (registered == null) {
            throw new BizException(400002, "Unknown command: " + cmdName);
        }

        // 创建执行上下文
        ExecuteContext ctx = new SimpleExecuteContext(req.getRoot(), req.getCommand());

        // 执行命令
        BashCommand command = registered.command();
        Object      result  = command.execute(ctx);
        return ServerResponse.success(result);
    }

    /**
     * 简单的执行上下文实现
     */
    private record SimpleExecuteContext(String root, String command) implements ExecuteContext {

        @Override
        public String getRoot() {

            return root;
        }

        @Override
        public String getCommand() {

            return command;
        }

    }

}
