package com.dev.lib.config;

import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.dev.lib.web.model.ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ServerResponseFastJsonNullExclusionTest.TestApplication.class,
        properties = "spring.application.name=starter-fastjson-test"
)
class ServerResponseFastJsonNullExclusionTest {

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Autowired
    private FastJsonHttpMessageConverter commonFastJsonHttpMessageConverter;

    @Test
    void shouldUseCommonCoreFastJsonSupportForMvcJsonSerialization() throws Exception {

        List<HttpMessageConverter<?>> converters = requestMappingHandlerAdapter.getMessageConverters();

        assertThat(converters).isNotEmpty();
        assertThat(converters.getFirst()).isSameAs(commonFastJsonHttpMessageConverter);
        assertThat(converters).anyMatch(FastJsonHttpMessageConverter.class::isInstance);
        assertThat(converters).noneMatch(converter -> converter.getClass().getName().contains("Jackson"));

        ServerResponse<String> response = ServerResponse.success("welcome, here is discount-server server!");
        String json = writeJson(response);

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
        String json = writeJson(response);

        assertThat(json).contains("\"code\":4101");
        assertThat(json).contains("\"message\":\"response failed\"");
        assertThat(json).contains("\"error\":\"参数校验失败\"");
    }

    private String writeJson(Object value) throws Exception {

        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        commonFastJsonHttpMessageConverter.write(value, MediaType.APPLICATION_JSON, outputMessage);
        return outputMessage.getBodyAsString();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
