package com.dev.lib.bash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bash 命令注册中心
 * 各模块通过此注册器注册命令处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BashCommandRegistry {

    /**
     * 命令名称 -> 命令处理器
     */
    private final Map<String, RegisteredCommand> commands = new ConcurrentHashMap<>();

    /**
     * 注册命令
     * @param name 命令名称
     * @param command 命令实例
     */
    public void register(String name, BashCommand command) {
        commands.put(name, new RegisteredCommand(name, command));
        log.debug("Registered bash command: {}", name);
    }

    /**
     * 获取所有已注册的命令
     */
    public Map<String, RegisteredCommand> getCommands() {
        return commands;
    }

    /**
     * 已注册的命令
     */
    public record RegisteredCommand(String name, BashCommand command) {}
}
