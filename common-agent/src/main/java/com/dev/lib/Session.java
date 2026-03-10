package com.dev.lib;

import com.dev.lib.core.message.ChatRequest;
import com.dev.lib.core.message.MessageDelt;
import com.dev.lib.core.message.MessageProcessor;
import com.dev.lib.core.message.MessageStream;
import com.dev.lib.core.session.ChatOptions;
import com.dev.lib.core.session.StreamCallback;
import com.dev.lib.lib.AgentError;
import com.dev.lib.lib.AgentException;
import com.dev.lib.util.parallel.ParallelExecutor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@RequiredArgsConstructor
public class Session {

    private final String id;

    private MessageStream messageStream;

    private MessageProcessor messageProcessor;

    private Set<String> clients = new LinkedHashSet<>();

    private List<StreamCallback> callbacks;

    void subscribe(StreamCallback callback) {

        if (callback != null) {
            callbacks.add(callback);
        }
    }

    public void send(ChatRequest chat, ChatOptions options) {

        clients.add(chat.getKey());

        try {
            messageStream.push(chat.getPrompt());
        } catch (InterruptedException e) {
            throw new AgentException(AgentError.MESSAGE_FULL);
        }
    }

    public void stream() {

        ParallelExecutor.with(callbacks)
                .apply(c -> {
                    c.onMessage(new MessageDelt());
                });
    }

    public void abort() {

    }

    public void interceptor() {

    }

}
