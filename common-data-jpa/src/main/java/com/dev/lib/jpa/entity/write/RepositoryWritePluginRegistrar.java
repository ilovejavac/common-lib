package com.dev.lib.jpa.entity.write;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.ArrayList;
import java.util.List;

public class RepositoryWritePluginRegistrar implements SmartInitializingSingleton, DisposableBean {

    private final ObjectProvider<RepositoryWritePlugin> plugins;

    private final List<RepositoryWritePluginChain.Registration> registrations = new ArrayList<>();

    public RepositoryWritePluginRegistrar(ObjectProvider<RepositoryWritePlugin> plugins) {

        this.plugins = plugins;
    }

    @Override
    public void afterSingletonsInstantiated() {

        plugins.orderedStream()
                .map(RepositoryWritePluginChain.getInstance()::register)
                .forEach(registrations::add);
    }

    @Override
    public void destroy() {

        registrations.forEach(RepositoryWritePluginChain.Registration::unregister);
        registrations.clear();
    }
}
