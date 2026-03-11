package com.dev.lib.agent.trigger.http.controller;

import com.dev.lib.agent.app.AgentChatAppService;
import com.dev.lib.agent.config.AgentProperties;
import com.dev.lib.agent.infra.agent.NoOpAgentExecutor;
import com.dev.lib.agent.infra.session.DefaultSessionManager;
import com.dev.lib.agent.infra.session.InMemorySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentChatControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        // 只组装控制器测试所需的最小依赖，避免把整个 Spring 容器拉起来。
        AgentProperties properties = new AgentProperties();
        AgentChatAppService appService = new AgentChatAppService(
                new DefaultSessionManager(
                        new InMemorySessionRepository(),
                        Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC)
                ),
                properties,
                new NoOpAgentExecutor()
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AgentChatController(appService))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    // 验证合法请求能够走完整个 HTTP 入参绑定和返回体序列化流程。
    void shouldAcceptValidChatRequest() throws Exception {

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "s-1",
                                  "prompt": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("s-1"))
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    // 验证空白 prompt 会被参数校验拦截，而不是继续进入应用层。
    void shouldRejectBlankPrompt() throws Exception {

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "s-1",
                                  "prompt": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
