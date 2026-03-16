package com.dev.lib.harness.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Map<String, AgentSession> SESSION_MAP = new ConcurrentHashMap<>();

    public AgentSession get(String id) {

        return SESSION_MAP.computeIfAbsent(id, it -> new AgentSession());
    }

}
