package com.dev.lib.encrypt.factory;

import com.dev.lib.encrypt.Encryptor;
import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.exceptions.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EncryptionStrategyFactory {
    private final Map<String, Encryptor> strategies;

    @Autowired
    public EncryptionStrategyFactory(List<Encryptor> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        Encryptor::getVersion,
                        s -> s,
                        (existing, replacement) -> {
                            log.warn(
                                    "发现重复的加密策略版本: {}, 使用: {}",
                                    existing.getVersion(),
                                    existing.getClass().getSimpleName()
                            );
                            return existing;  // 保留第一个
                        }
                ));

        log.info("加载加密策略: {}", strategies.keySet());
    }

    public Encryptor getStrategy(EncryptVersion version) {
        return getStrategy(version.getMsg());
    }

    public Encryptor getStrategy(String version) {
        Encryptor service = strategies.get(version);
        if (service == null) {
            log.error("未找到加密策略: {}, 可用策略: {}", version, strategies.keySet());
            throw new BizException(404, "不存在加密服务: " + version);
        }
        return service;
    }

    /**
     * 获取所有可用策略
     */
    public Map<String, Encryptor> getAllStrategies() {
        return Map.copyOf(strategies);
    }

    /**
     * 检查策略是否存在
     */
    public boolean hasStrategy(String version) {
        return strategies.containsKey(version);
    }
}