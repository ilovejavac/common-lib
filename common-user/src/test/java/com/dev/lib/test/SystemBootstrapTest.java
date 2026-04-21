package com.dev.lib.test;

import com.dev.lib.biz.bootstrap.BootStrapException;
import com.dev.lib.biz.bootstrap.BootstrapError;
import com.dev.lib.biz.bootstrap.model.BootstrapCmd;
import com.dev.lib.biz.bootstrap.repo.IBootstrapQueryRepo;
import com.dev.lib.biz.bootstrap.service.BootstrapSystemUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SystemBootstrapTest {

    @Mock
    private IBootstrapQueryRepo bootstrapQueryRepo;

    @InjectMocks
    private BootstrapSystemUseCase bootstrapSystemUseCase;

    @Test
    @DisplayName("测试系统未初始化")
    public void bootstrapSystemWhenUnInitialize() {

        when(bootstrapQueryRepo.isSystemInitialized()).thenReturn(false);
        BootstrapCmd.BootstrapSystem cmd = new BootstrapCmd.BootstrapSystem();
        cmd.setMail("admin@test.com").setPassword("1q2w3e4r5t");

        Boolean result = bootstrapSystemUseCase.execute(cmd);

        assertTrue(result);
    }

    @Test
    @DisplayName("测试系统已初始化")
    public void bootstrapSystemWhenInitialized() {

        when(bootstrapQueryRepo.isSystemInitialized()).thenReturn(true);
        BootstrapCmd.BootstrapSystem cmd = new BootstrapCmd.BootstrapSystem();
        cmd.setMail("admin@test.com").setPassword("1q2w3e4r5t");

        var exception = assertThrows(
                BootStrapException.class, () -> {
                    bootstrapSystemUseCase.execute(cmd);
                }
        );
        assertEquals(BootstrapError.SYSTEM_HAS_BEEN_INITIALIZED.getMessage(), exception.getMessage());
    }

}
