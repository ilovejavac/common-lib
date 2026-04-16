package com.dev.lib.config;

import com.dev.lib.web.model.ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ServerResponseJacksonNullExclusionTest.TestApplication.class,
        properties = "spring.application.name=starter-jackson-test"
)
class ServerResponseJacksonNullExclusionTest {

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Autowired
    private JacksonJsonHttpMessageConverter commonJacksonJsonHttpMessageConverter;

    @Test
    void shouldUseCommonCoreJacksonSupportForMvcJsonSerialization() throws Exception {

        List<HttpMessageConverter<?>> converters = requestMappingHandlerAdapter.getMessageConverters();

        assertThat(converters).isNotEmpty();
        assertThat(converters.getFirst()).isSameAs(commonJacksonJsonHttpMessageConverter);
        assertThat(converters).anyMatch(JacksonJsonHttpMessageConverter.class::isInstance);

        ServerResponse<String> response = ServerResponse.success("welcome, here is discount-server server!");
        String json = commonJacksonJsonHttpMessageConverter.getMapper().writeValueAsString(response);

        assertThat(json).contains("\"code\":200");
        assertThat(json).contains("\"message\":\"success\"");
        assertThat(json).contains("\"data\":\"welcome, here is discount-server server!\"");
        assertThat(json).doesNotContain("\"error\":null");
        assertThat(json).doesNotContain("\"pager\":null");
        assertThat(json).doesNotContain("\"traceId\":null");
    }

    @Test
    void shouldSerializeFailureMessageIntoMessageField() throws Exception {

        ServerResponse<Void> response = ServerResponse.fail(4101, "参数校验失败");
        String json = commonJacksonJsonHttpMessageConverter.getMapper().writeValueAsString(response);

        assertThat(json).contains("\"code\":4101");
        assertThat(json).contains("\"message\":\"参数校验失败\"");
        assertThat(json).contains("\"error\":\"参数校验失败\"");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
