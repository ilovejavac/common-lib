package com.dev.lib.agent.infra.stream;

import com.dev.lib.agent.config.AgentProperties;
import com.dev.lib.agent.domain.model.AgentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
public class SseSessionStreamHub implements SessionStreamHub {

    private final AgentProperties properties;
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String sessionId) {

        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());
        emitters.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(error -> removeEmitter(sessionId, emitter));

        return emitter;
    }

    @Override
    public void publish(String sessionId, AgentMessage message) {

        List<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null) {
            return;
        }

        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(message.getType().name())
                        .id(message.getMessageId())
                        .data(message));
            } catch (IOException e) {
                log.warn("failed to publish session event, sessionId={}", sessionId, e);
                removeEmitter(sessionId, emitter);
            }
        }
    }

    @Override
    public void complete(String sessionId) {

        List<SseEmitter> sessionEmitters = emitters.remove(sessionId);
        if (sessionEmitters == null) {
            return;
        }
        for (SseEmitter emitter : sessionEmitters) {
            emitter.complete();
        }
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {

        List<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null) {
            return;
        }
        sessionEmitters.remove(emitter);
        if (sessionEmitters.isEmpty()) {
            emitters.remove(sessionId);
        }
    }
}
