package com.dev.lib.web.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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

    @Test
    void shouldKeepPageMetadataWithinFrontendVisibleRange() {

        Page<String> page = new PageImpl<>(
                List.of("first"),
                PageRequest.of(0, 1),
                2
        );

        ServerResponse<List<String>> response = ServerResponse.success(page);

        assertThat(response.getData()).containsExactly("first");
        assertThat(response.getPager().getTotal()).isEqualTo(2);
        assertThat(response.getPager().getHasNext()).isTrue();
    }

    @Test
    void shouldLimitPageMetadataWithoutTrimmingContent() {

        Page<String> page = new PageImpl<>(
                Collections.nCopies(537, "row"),
                PageRequest.of(65, 1000),
                100000
        );

        ServerResponse<List<String>> response = ServerResponse.success(page);

        assertSoftly(softly -> {
            softly.assertThat(response.getData()).hasSize(537);
            softly.assertThat(response.getPager().getPage()).isEqualTo(66);
            softly.assertThat(response.getPager().getSize()).isEqualTo(1000);
            softly.assertThat(response.getPager().getTotal()).isEqualTo(65536);
            softly.assertThat(response.getPager().getHasNext()).isFalse();
        });
    }

    @Test
    void shouldReturnEmptyDataForPageOutsideFrontendVisibleRange() {

        Page<String> page = new PageImpl<>(
                List.of(),
                PageRequest.of(66, 1000),
                100000
        );

        ServerResponse<List<String>> response = ServerResponse.success(page);

        assertSoftly(softly -> {
            softly.assertThat(response.getData()).isEmpty();
            softly.assertThat(response.getPager().getPage()).isEqualTo(67);
            softly.assertThat(response.getPager().getSize()).isEqualTo(1000);
            softly.assertThat(response.getPager().getTotal()).isEqualTo(65536);
            softly.assertThat(response.getPager().getHasNext()).isFalse();
        });
    }
}
