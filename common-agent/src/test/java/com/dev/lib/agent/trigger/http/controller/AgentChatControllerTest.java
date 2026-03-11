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
