package com.dev.lib.core.message;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageStream {

    private BlockingQueue<String> queue = new LinkedBlockingQueue<>(32);

    public void push(String prompt) throws InterruptedException {
        queue.put(prompt);
    }


}
