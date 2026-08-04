package com.dev.lib.config;

import com.dev.lib.web.model.ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import tools.jackson.databind.json.JsonMapper;

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
    private JsonMapper jsonMapper;

    @Test
    void shouldUseSpringJacksonConverterWithCommonJsonMapper() throws Exception {

        List<HttpMessageConverter<?>> converters = requestMappingHandlerAdapter.getMessageConverters();
        JacksonJsonHttpMessageConverter converter = converters.stream()
                .filter(JacksonJsonHttpMessageConverter.class::isInstance)
                .map(JacksonJsonHttpMessageConverter.class::cast)
                .findFirst()
                .orElse(null);

        assertThat(converters).isNotEmpty();
        assertThat(converter).isNotNull();
        assertThat(converter.getMapper()).isSameAs(jsonMapper);
        assertThat(converters).noneMatch(item -> item.getClass().getName().contains("FastJson"));

        ServerResponse<String> response = ServerResponse.success("welcome, here is discount-server server!");
        String json = writeJson(converter, response);

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
        String json = writeJson(jacksonConverter(), response);

        assertThat(json).contains("\"code\":4101");
        assertThat(json).contains("\"message\":\"response failed\"");
        assertThat(json).contains("\"error\":\"参数校验失败\"");
    }

    private JacksonJsonHttpMessageConverter jacksonConverter() {

        return requestMappingHandlerAdapter.getMessageConverters().stream()
                .filter(JacksonJsonHttpMessageConverter.class::isInstance)
                .map(JacksonJsonHttpMessageConverter.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private String writeJson(JacksonJsonHttpMessageConverter converter, Object value) throws Exception {

        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(value, MediaType.APPLICATION_JSON, outputMessage);
        return outputMessage.getBodyAsString();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
