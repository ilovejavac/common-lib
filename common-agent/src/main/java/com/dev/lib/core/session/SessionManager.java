package com.dev.lib.core.session;

import com.dev.lib.Session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>(2048);

    private static final Map<String, List<String>> clients = new ConcurrentHashMap<>();

    public static Session open(String sessionId) {

        return SESSION_MAP.computeIfAbsent(sessionId, id -> new Session(id));
    }

}
