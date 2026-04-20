package com.dev.lib.web.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServerResponseTest {

    @Test
    void shouldWriteJsonToServletResponse() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServerResponse.success("ok").to(response);

        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":200");
        assertThat(response.getContentAsString()).contains("\"message\":\"success\"");
        assertThat(response.getContentAsString()).contains("\"data\":\"ok\"");
    }

    @Test
    void shouldReturnSuccessEnvelopeForNullPage() {

        ServerResponse<List<String>> response = ServerResponse.success((Page<String>) null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isNull();
        assertThat(response.getPager()).isNull();
    }
}
