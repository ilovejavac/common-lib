package com.dev.lib.core.session;

import com.dev.lib.core.message.MessageDelt;

@FunctionalInterface
public interface StreamCallback {
    void onMessage(MessageDelt delt);
}
