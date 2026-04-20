package com.dev.lib.biz.bootstrap.service;

import com.dev.lib.biz.bootstrap.model.BootstrapCmd;
import com.dev.lib.biz.bootstrap.repo.IBootstrapQueryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapSystemUseCase {

    private final IBootstrapQueryRepo bootstrapQueryRepo;

    public Boolean execute(BootstrapCmd.BootstrapSystem cmd) {

        throw new UnsupportedOperationException();
    }
}
