package com.dev.lib.scheduler;

import com.dev.lib.security.service.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenClear {

    private final TokenManager tokenManager;

    @Scheduled(cron = "0 0 0 * * *")
    public void clear() {

        try {
            log.info(
                    "clear {} Expired tokens",
                    tokenManager.cleanExpiredTokens()
            );
        } catch (Exception e) {
            log.warn(
                    "clear expired token fail",
                    e
            );
        }
    }

}
