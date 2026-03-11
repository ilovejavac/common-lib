package com.dev.lib.agent.domain.model;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class AgentSession {

    private final String sessionId;
    private final Instant createdAt;
    private final Deque<PendingUserMessage> pendingQueue = new LinkedBlockingDeque<>();
    private final LinkedList<AgentMessage> history = new LinkedList<>();
    private SessionStatus status;
    private Instant lastActiveAt;
    private boolean closed;

    private AgentSession(String sessionId, Instant now) {

        this.sessionId = sessionId;
        this.createdAt = now;
        this.lastActiveAt = now;
        this.status = SessionStatus.IDLE;
    }

    public static AgentSession create(String sessionId, Instant now) {

        return new AgentSession(sessionId, now);
    }

    public void appendHistory(AgentMessage message, int maxHistoryMessages) {

        assertOpen();
        history.add(message);
        trimHistory(maxHistoryMessages);
        touch(message.timestamp());
    }

    public void enqueue(PendingUserMessage message, int maxPendingMessages) {

        assertOpen();
        if (pendingQueue.size() >= maxPendingMessages) {
            throw new IllegalStateException("pending queue is full");
        }
        pendingQueue.addLast(message);
        touch(message.timestamp());
    }

    public PendingUserMessage pollPending() {

        return pendingQueue.pollFirst();
    }

    public void markRunning(Instant now) {

        assertOpen();
        this.status = SessionStatus.RUNNING;
        touch(now);
    }

    public void markIdle(Instant now) {

        assertOpen();
        this.status = SessionStatus.IDLE;
        touch(now);
    }

    public void close() {

        this.closed = true;
        this.status = SessionStatus.CLOSED;
    }

    public String getSessionId() {

        return sessionId;
    }

    public SessionStatus getStatus() {

        return status;
    }

    public Deque<PendingUserMessage> getPendingQueue() {

        return new ArrayDeque<>(pendingQueue);
    }

    public List<AgentMessage> getHistory() {

        return Collections.unmodifiableList(history);
    }

    public Instant getLastActiveAt() {

        return lastActiveAt;
    }

    public Instant getCreatedAt() {

        return createdAt;
    }

    public boolean isClosed() {

        return closed;
    }

    private void trimHistory(int maxHistoryMessages) {

        int nonSystemCount = (int) history.stream()
                .filter(message -> message.role() != MessageRole.SYSTEM)
                .count();

        while (nonSystemCount > maxHistoryMessages) {
            AgentMessage oldest = history.stream()
                    .filter(message -> message.role() != MessageRole.SYSTEM)
                    .findFirst()
                    .orElse(null);

            if (oldest == null) {
                break;
            }
            history.remove(oldest);
            nonSystemCount--;
        }
    }

    private void touch(Instant now) {

        this.lastActiveAt = now;
    }

    private void assertOpen() {

        if (closed) {
            throw new IllegalStateException("session is closed");
        }
    }
}
