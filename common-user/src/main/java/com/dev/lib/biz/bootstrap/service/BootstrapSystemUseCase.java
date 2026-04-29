package com.dev.lib.biz.bootstrap.service;

import com.dev.lib.biz.bootstrap.BootStrapException;
import com.dev.lib.biz.bootstrap.BootstrapError;
import com.dev.lib.biz.bootstrap.model.BootstrapCmd;
import com.dev.lib.biz.bootstrap.repo.IBootstrapQueryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapSystemUseCase {

    private final IBootstrapQueryRepo bootstrapQueryRepo;

    public Boolean execute(@Validated BootstrapCmd.BootstrapSystem cmd) {

        if (Boolean.TRUE.equals(bootstrapQueryRepo.isSystemInitialized())) {
            throw new BootStrapException(BootstrapError.SYSTEM_HAS_BEEN_INITIALIZED);
        }
        return true;
    }
}
