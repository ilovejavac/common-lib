package com.dev.lib.task.biz.dispatcher;

import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.util.Jsons;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryDecisionTest {

    @Test
    void shouldRetryWithFixedDelayForAsyncPolicy() {

        RetryPolicy policy = RetryPolicy.builder()
                .maxRetry(2)
                .fixedDelay(Duration.ofSeconds(10))
                .exponentialBackoff(false)
                .build();

        RetryDecision decision = RetryDecision.next(Jsons.toJson(policy), 1, true);

        assertTrue(decision.shouldRetry());
        assertTrue(decision.nextTime().isAfter(java.time.LocalDateTime.now().plusSeconds(9)));
    }

    @Test
    void shouldRetryWithExponentialDelayForReliablePolicy() {

        RetryPolicy policy = RetryPolicy.builder()
                .maxRetry(10)
                .initialDelay(Duration.ofSeconds(2))
                .exponentialBackoff(true)
                .build();

        RetryDecision decision = RetryDecision.next(Jsons.toJson(policy), 3, true);

        assertTrue(decision.shouldRetry());
        assertTrue(decision.nextTime().isAfter(java.time.LocalDateTime.now().plusSeconds(7)));
    }

    @Test
    void shouldStopRetryingWhenRetryBudgetIsExhausted() {

        RetryPolicy policy = RetryPolicy.builder()
                .maxRetry(2)
                .fixedDelay(Duration.ofSeconds(10))
                .exponentialBackoff(false)
                .build();

        RetryDecision decision = RetryDecision.next(Jsons.toJson(policy), 3, true);

        assertFalse(decision.shouldRetry());
    }
}
