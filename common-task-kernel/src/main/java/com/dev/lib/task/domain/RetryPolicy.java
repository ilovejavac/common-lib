package com.dev.lib.task.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder
public class RetryPolicy {

    private final Integer maxRetry;

    private final Duration fixedDelay;

    private final Duration initialDelay;

    private final Boolean exponentialBackoff;

}
