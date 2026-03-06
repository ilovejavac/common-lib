package com.dev.lib.bash;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 注册 common-bash 内置命令。
 */
@Component
@RequiredArgsConstructor
public class BuiltinBashCommandFactory implements InitializingBean {

    private final BashCommandRegistry registry;

    @Override
    public void afterPropertiesSet() {

        registry.register("bash", new BuiltinBashCommand());
    }
}
