package com.dev.lib.storage.domain.command;

import com.dev.lib.bash.BashCommandRegistry;
import com.dev.lib.storage.domain.command.vfs.*;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
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

    private final VirtualFileSystem vfs;

    private final BashCommandRegistry registry;

    /**
     * 启动时注册所有 VFS 命令
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        registry.register("ls", new LsCommand(vfs));
        registry.register("cat", new CatCommand(vfs));
        registry.register("head", new HeadCommand(vfs));
        registry.register("tail", new TailCommand(vfs));
        registry.register("echo", new EchoCommand(vfs));
        registry.register("write", new WriteCommand(vfs));
        registry.register("touch", new TouchCommand(vfs));
        registry.register("cp", new CpCommand(vfs));
        registry.register("mv", new MvCommand(vfs));
        registry.register("rm", new RmCommand(vfs));
        registry.register("mkdir", new MkdirCommand(vfs));
        registry.register("find", new FindCommand(vfs));
        registry.register("grep", new GrepCommand(vfs));
        registry.register("sed", new SedCommand(vfs));
    }

}
