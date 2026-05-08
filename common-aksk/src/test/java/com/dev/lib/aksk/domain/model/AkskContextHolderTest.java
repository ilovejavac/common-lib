package com.dev.lib.aksk.domain.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AkskContextHolderTest {

    @AfterEach
    void tearDown() {

        AkskContextHolder.clear();
    }

    @Test
    void setGetAndCurrentShouldReturnAuthenticationFromCurrentThread() {

        AkskAuthentication authentication = new AkskAuthentication();
        authentication.setCredentialId("cred-1");
        authentication.setAccessKey("ak_valid");

        AkskContextHolder.set(authentication);

        assertThat(AkskContextHolder.get()).isSameAs(authentication);
        assertThat(AkskContextHolder.current()).isSameAs(authentication);
        assertThat(AkskContextHolder.isAuthenticated()).isTrue();
    }

    @Test
    void currentShouldReturnAnonymousValueWhenNoAkskAuthenticationExists() {

        assertThat(AkskContextHolder.get()).isNull();
        assertThat(AkskContextHolder.current().getAccessKey()).isEqualTo("anonymous");
        assertThat(AkskContextHolder.isAuthenticated()).isFalse();
    }

    @Test
    void clearShouldRemoveCurrentAuthentication() {

        AkskAuthentication authentication = new AkskAuthentication();
        authentication.setCredentialId("cred-1");
        AkskContextHolder.set(authentication);

        AkskContextHolder.clear();

        assertThat(AkskContextHolder.get()).isNull();
        assertThat(AkskContextHolder.isAuthenticated()).isFalse();
    }
}
