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
        // 文件读取类
        registry.register("cat", new CatCommand());
        registry.register("head", new HeadCommand());
        registry.register("tail", new TailCommand());
        registry.register("wc", new WcBashCommand());

        // 文本处理类
        registry.register("grep", new GrepCommand());
        registry.register("sed", new SedBashCommand());
        registry.register("cut", new CutBashCommand());
        registry.register("sort", new SortBashCommand());
        registry.register("uniq", new UniqBashCommand());

        // 文件操作类
        registry.register("touch", new TouchCommand());
        registry.register("cp", new CpCommand());
        registry.register("mv", new MvCommand());
        registry.register("rm", new RmCommand());

        // 目录操作类
        registry.register("ls", new LsCommand());
        registry.register("mkdir", new MkdirCommand());
        registry.register("pwd", new PwdBashCommand());
        registry.register("tree", new TreeBashCommand());

        // 搜索类
        registry.register("find", new FindCommand());

        // 文件信息类
        registry.register("stat", new StatBashCommand());
        registry.register("diff", new DiffBashCommand());

        // 完整 bash 支持（管道、变量、控制流等）
        registry.register("vfsbash", new VfsBashCommand());
    }
}
