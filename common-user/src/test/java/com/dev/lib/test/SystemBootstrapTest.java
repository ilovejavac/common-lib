package com.dev.lib.test;

import com.dev.lib.biz.bootstrap.model.BootstrapCmd;
import com.dev.lib.biz.bootstrap.repo.IBootstrapQueryRepo;
import com.dev.lib.biz.bootstrap.service.BootstrapSystemUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SystemBootstrapTest {

    @Mock
    private IBootstrapQueryRepo bootstrapQueryRepo;

    @InjectMocks
    private BootstrapSystemUseCase bootstrapSystemUseCase;

    @Test
    @DisplayName("测试系统未初始化")
    public void bootstrapSystemWhenUnInitialize() {
        BootstrapCmd.BootstrapSystem cmd = new BootstrapCmd.BootstrapSystem();

        Boolean result = bootstrapSystemUseCase.execute(cmd);

        assertTrue(result);
    }
}
