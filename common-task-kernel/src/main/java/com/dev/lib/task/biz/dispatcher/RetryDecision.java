package com.dev.lib.task.biz.dispatcher;

import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.util.Jsons;

import java.time.LocalDateTime;

public record RetryDecision(boolean shouldRetry, LocalDateTime nextTime) {

    public static RetryDecision next(String retryPolicyJson, int currentAttemptNo, boolean retryable) {

        if (!retryable) {
            return new RetryDecision(false, null);
        }
        RetryPolicy policy = Jsons.parse(retryPolicyJson, RetryPolicy.class);
        if (policy.getMaxRetry() == null || currentAttemptNo > policy.getMaxRetry()) {
            return new RetryDecision(false, null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(policy.getExponentialBackoff()) && policy.getInitialDelay() != null) {
            long seconds = policy.getInitialDelay().toSeconds() * (1L << Math.max(0, currentAttemptNo - 1));
            return new RetryDecision(true, now.plusSeconds(seconds));
        }
        long seconds = policy.getFixedDelay() == null ? 0 : policy.getFixedDelay().toSeconds();
        return new RetryDecision(true, now.plusSeconds(seconds));
    }

}
