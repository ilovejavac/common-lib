package com.dev.lib.cache;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonJsonRedissonCodecTest {

    @Test
    void shouldEncodeAndDecodePolymorphicPayload() throws Exception {

        JacksonJsonRedissonCodec codec = new JacksonJsonRedissonCodec();
        Payload payload = new Payload();
        payload.id = 9007199254740992L;
        payload.amount = new BigDecimal("1.2");
        payload.createdAt = Instant.parse("2026-03-11T04:05:06Z");
        payload.localDateTime = LocalDateTime.parse("2026-03-11T12:05:06");
        payload.localDate = LocalDate.parse("2026-03-11");
        payload.localTime = LocalTime.parse("12:05:06");
        payload.tags = Map.of("name", "demo");

        ByteBuf encoded = (ByteBuf) codec.getValueEncoder().encode(payload);
        Object decoded = codec.getValueDecoder().decode(encoded, null);

        assertThat(decoded).isInstanceOf(Payload.class);
        Payload restored = (Payload) decoded;
        assertThat(restored.id).isEqualTo(payload.id);
        assertThat(restored.amount).isEqualByComparingTo("1.200000");
        assertThat(restored.createdAt).isEqualTo(payload.createdAt);
        assertThat(restored.localDateTime).isEqualTo(payload.localDateTime);
        assertThat(restored.localDate).isEqualTo(payload.localDate);
        assertThat(restored.localTime).isEqualTo(payload.localTime);
        assertThat(restored.tags).containsEntry("name", "demo");
    }

    static class Payload {

        public Long id;

        public BigDecimal amount;

        public Instant createdAt;

        public LocalDateTime localDateTime;

        public LocalDate localDate;

        public LocalTime localTime;

        public Map<String, Object> tags;
    }
}
