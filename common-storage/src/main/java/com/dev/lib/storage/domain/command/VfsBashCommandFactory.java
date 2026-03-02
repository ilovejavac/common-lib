package com.dev.lib.storage.domain.command;

import com.dev.lib.bash.BashCommandRegistry;
import com.dev.lib.storage.domain.command.vfs.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * VFS Bash 命令注册器
 * 负责注册所有 VFS 命令到 BashCommandRegistry
 */
@Component
@RequiredArgsConstructor
public class VfsBashCommandFactory implements InitializingBean {

    private final BashCommandRegistry registry;

    /**
     * 启动时注册所有 VFS 命令
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        registry.register("ls", new LsCommand());
        registry.register("cat", new CatCommand());
        registry.register("head", new HeadCommand());
        registry.register("tail", new TailCommand());
        registry.register("echo", new EchoCommand());
        registry.register("write", new WriteCommand());
        registry.register("touch", new TouchCommand());
        registry.register("cp", new CpCommand());
        registry.register("mv", new MvCommand());
        registry.register("rm", new RmCommand());
        registry.register("mkdir", new MkdirCommand());
        registry.register("find", new FindCommand());
        registry.register("grep", new GrepCommand());
        registry.register("sed", new SedCommand());
    }

}
