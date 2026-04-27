package com.dev.lib.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dev.lib.handler.ExceptionHandle;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingFilterTest {

    private Logger loggingFilterLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {

        loggingFilterLogger = (Logger) LoggerFactory.getLogger(LoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        loggingFilterLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {

        loggingFilterLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void shouldNotLogRequestBodyWhenRequestCompletesWithoutHandledException() throws Exception {

        executeFilter(null);

        assertThat(completedLog()).doesNotContain("request_body");
    }

    @Test
    void shouldLogRequestBodyWhenRequestHasHandledExceptionMarker() throws Exception {

        executeFilter(Boolean.TRUE);

        assertThat(completedLog())
                .contains("request_body")
                .contains("alice");
    }

    private void executeFilter(Boolean handledException) throws ServletException, IOException {

        LoggingFilter filter = new LoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setServletPath("/api/users");
        request.setContentType("application/json");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent("{\"name\":\"alice\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            if (handledException != null) {
                servletRequest.setAttribute(ExceptionHandle.EXCEPTION_HANDLED_ATTRIBUTE, handledException);
            }
        };

        filter.doFilter(request, response, chain);
    }

    private String completedLog() {

        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .filter(event -> "Request completed".equals(event.getFormattedMessage()))
                .map(event -> {
                    StringBuilder value = new StringBuilder(event.getFormattedMessage());
                    Object[] arguments = event.getArgumentArray();
                    if (arguments != null) {
                        for (Object argument : arguments) {
                            value.append(' ').append(argument);
                        }
                    }
                    return value.toString();
                })
                .findFirst()
                .orElse("");
    }
}
