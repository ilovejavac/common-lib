package com.dev.lib.web;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.handler.ExceptionHandle;
import com.dev.lib.web.model.ServerResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(
        classes = GlobalExceptionHandlingTest.TestApplication.class,
        properties = "spring.application.name=global-exception-test"
)
class GlobalExceptionHandlingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldExposeBizExceptionMessageInMessageField() throws Exception {

        mockMvc.perform(get("/api/exception/biz"))
                .andExpect(jsonPath("$.code").value(4999))
                .andExpect(jsonPath("$.message").value("response failed"))
                .andExpect(jsonPath("$.error").value("业务失败"));
    }

    @Test
    void shouldMaskUnexpectedJavaExceptions() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/exception/runtime"))
                .andExpect(jsonPath("$.code").value(5500))
                .andExpect(jsonPath("$.message").value("response failed"))
                .andExpect(jsonPath("$.error").value("系统繁忙，请稍后再试"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("database connection refused")
                .doesNotContain("RuntimeException");
    }

    @Test
    void shouldMarkRequestWhenExceptionIsHandled() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/exception/runtime"))
                .andExpect(jsonPath("$.code").value(5500))
                .andReturn();

        assertThat(result.getRequest().getAttribute(ExceptionHandle.EXCEPTION_HANDLED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldNotLeakIllegalArgumentParserMessage() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/exception/illegal-argument"))
                .andExpect(jsonPath("$.code").value(4105))
                .andExpect(jsonPath("$.message").value("request failed"))
                .andExpect(jsonPath("$.error").value("参数错误"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("For input string");
    }

    @Test
    void shouldUseReservedCodeForValidationFailures() throws Exception {

        mockMvc.perform(post("/api/exception/validated")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(jsonPath("$.code").value(4101))
                .andExpect(jsonPath("$.message").value("request failed"))
                .andExpect(jsonPath("$.error").value("参数校验失败"))
                .andExpect(jsonPath("$.data.name").value("名称不能为空"));
    }

    @Test
    void shouldUseReservedCodeForMissingRequestParameter() throws Exception {

        mockMvc.perform(get("/api/exception/required-param"))
                .andExpect(jsonPath("$.code").value(4103))
                .andExpect(jsonPath("$.message").value("request failed"))
                .andExpect(jsonPath("$.error").value("缺少必需参数：name"));
    }

    @Test
    void shouldConvertMissingRouteToUnifiedNotFoundPayload() throws Exception {

        mockMvc.perform(get("/api/exception/not-found"))
                .andExpect(jsonPath("$.code").value(4304))
                .andExpect(jsonPath("$.message").value("request failed"))
                .andExpect(jsonPath("$.error").value("接口不存在"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestExceptionController.class)
    static class TestApplication {
    }

    @RestController
    static class TestExceptionController {

        @GetMapping("/api/exception/biz")
        ServerResponse<Void> biz() {

            throw new BizException(4999, "业务失败");
        }

        @GetMapping("/api/exception/runtime")
        ServerResponse<Void> runtime() {

            throw new RuntimeException("database connection refused");
        }

        @GetMapping("/api/exception/illegal-argument")
        ServerResponse<Void> illegalArgument() {

            throw new IllegalArgumentException("For input string: \"abc\"");
        }

        @GetMapping("/api/exception/required-param")
        ServerResponse<Void> requiredParam(@RequestParam String name) {

            return ServerResponse.ok();
        }

        @PostMapping("/api/exception/validated")
        ServerResponse<Void> validated(@Valid @RequestBody ValidationRequest request) {

            return ServerResponse.ok();
        }
    }

    static class ValidationRequest {

        @NotBlank(message = "名称不能为空")
        private String name;

        public String getName() {

            return name;
        }

        public void setName(String name) {

            this.name = name;
        }
    }
}
