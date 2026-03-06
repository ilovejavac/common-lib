package com.dev.lib.bash.vfs;

import com.dev.lib.bash.BashCommandRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * VFS 命令注册工厂
 * 注册所有基于 VFS 的文件操作命令
 */
@Component
@RequiredArgsConstructor
public class VfsCommandFactory implements InitializingBean {

    private final BashCommandRegistry registry;

    @Override
    public void afterPropertiesSet() {
        // 文件查看
        registry.register("ls", new LsCommand());
        registry.register("cat", new CatCommand());
        registry.register("head", new HeadCommand());
        registry.register("tail", new TailCommand());

        // 文件搜索
        registry.register("find", new FindCommand());
        registry.register("grep", new GrepCommand());

        // 文件操作
        registry.register("touch", new TouchCommand());
        registry.register("mkdir", new MkdirCommand());
        registry.register("rm", new RmCommand());
        registry.register("cp", new CpCommand());
        registry.register("mv", new MvCommand());

        // 完整 bash 支持（管道、变量、控制流等）
        registry.register("vfsbash", new VfsBashCommand());
    }
}
