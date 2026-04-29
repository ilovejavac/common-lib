package com.dev.lib.jpa.handler;

import com.dev.lib.web.MessageUtils;
import com.dev.lib.web.model.ServerResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class DatabaseExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeAll
    static void initMessages() {

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:i18n/messages", "classpath:i18n/common");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        new MessageUtils().setMessageSource(messageSource);
    }

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new DatabaseExceptionHandler())
                .build();
    }

    @Test
    void shouldUseReservedCodeForDuplicateKeyErrors() throws Exception {

        mockMvc.perform(get("/database/duplicate"))
                .andExpect(jsonPath("$.code").value(5101))
                .andExpect(jsonPath("$.message").value("response failed"))
                .andExpect(jsonPath("$.error").value("数据已存在，请勿重复提交"));
    }

    @Test
    void shouldMaskGenericDataAccessDetails() throws Exception {

        mockMvc.perform(get("/database/generic"))
                .andExpect(jsonPath("$.code").value(5103))
                .andExpect(jsonPath("$.message").value("response failed"))
                .andExpect(jsonPath("$.error").value("数据库操作异常，请稍后重试"));
    }

    @RestController
    static class TestController {

        @GetMapping("/database/duplicate")
        ServerResponse<Void> duplicate() {

            throw new DuplicateKeyException("Duplicate entry 'sku-1'");
        }

        @GetMapping("/database/generic")
        ServerResponse<Void> generic() {

            throw new RecoverableDataAccessException("Connection refused");
        }
    }
}
