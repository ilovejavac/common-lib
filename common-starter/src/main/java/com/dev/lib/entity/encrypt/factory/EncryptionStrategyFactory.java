package com.dev.lib.entity.encrypt.factory;

import com.dev.lib.entity.encrypt.EncryptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EncryptionStrategyFactory {
    private final Map<String, EncryptService> strategies;

    @Autowired
    public EncryptionStrategyFactory(List<EncryptService> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(EncryptService::getVersion, s -> s));
    }

    public EncryptService getStrategy(String version) {
        return strategies.getOrDefault(version, strategies.get("v1"));
    }
}