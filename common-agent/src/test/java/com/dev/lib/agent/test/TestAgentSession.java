package com.dev.lib.agent.test;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.harness.session.AgentSession;
import com.dev.lib.harness.session.SessionManager;
import com.dev.lib.harness.session.model.Submission;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;

@Slf4j
public class TestAgentSession {

    @Test
    public void testSession() {
        SessionManager sm = new SessionManager();

        AgentSession session = sm.get("1");

        for (int i = 0; i < 100; i++) {
            int finalI = i;
            session.submit(new Submission() {
                @Override
                public String getId() {
                    return "test-"+ finalI;
                }
            });

            try {
                Thread.sleep(50);
            } catch (Exception e) {

            }
        }

        session.destroy();
    }
}
