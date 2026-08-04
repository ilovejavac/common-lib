package com.dev.lib.config;

import com.dev.lib.util.Jsons;
import com.dev.lib.web.sensitive.Sensitive;
import com.dev.lib.web.sensitive.SensitiveType;
import com.dev.lib.web.serialize.PopulateContextHolder;
import com.dev.lib.web.serialize.PopulateField;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonSupportTest {

    @Test
    void shouldSerializeUsingExpectedCommonRules() {

        Payload payload = payload();

        String json = Jsons.toJson(payload);
        Payload restored = Jsons.parse(json, Payload.class);

        assertThat(json).contains("\"amount\":12.300000");
        assertThat(json).contains("\"createdAt\":\"2026-03-11 12:05:06\"");
        assertThat(json).contains("\"largeId\":\"9007199254740992\"");
        assertThat(json).contains("\"status\":\"enabled\"");
        assertThat(json).contains("\"phone\":\"138****5678\"");
        assertThat(json).contains("\"emptyName\":\"\"");
        assertThat(json).contains("\"ownerIdInfo\":{\"name\":\"Ada\"}");
        assertThat(json).doesNotContain("13812345678");
        assertThat(json).doesNotContain("optionalNote");
        assertThat(restored.amount).isEqualByComparingTo("12.300000");
        assertThat(restored.createdAt).isEqualTo(Instant.parse("2026-03-11T04:05:06Z"));
        assertThat(restored.status).isEqualTo(Status.ENABLED);
    }

    @Test
    void shouldParseJacksonTypeReference() {

        String json = "{\"demo\":{\"amount\":12.300000,\"createdAt\":\"2026-03-11 12:05:06\"}}";

        Map<String, Payload> restored = Jsons.parse(json, new TypeReference<>() {
        });

        assertThat(restored.get("demo").amount).isEqualByComparingTo("12.300000");
        assertThat(restored.get("demo").createdAt).isEqualTo(Instant.parse("2026-03-11T04:05:06Z"));
    }

    @Test
    void shouldReadTreeAsJacksonNode() {

        JsonNode tree = Jsons.readTree("{\"code\":200}");

        assertThat(tree.get("code").asInt()).isEqualTo(200);
    }

    @Test
    void shouldNotExposeOrActivatePolymorphicTypeLoadingInJsonUtility() {

        String json = "{\"@class\":\"" + Payload.class.getName() + "\",\"largeId\":7}";

        Object restored = Jsons.parse(json);
        boolean exposesTypeLoadingApi = Arrays.stream(Jsons.class.getDeclaredMethods())
                .map(method -> method.getName())
                .anyMatch(name -> name.equals("writeWithType") || name.equals("parseWithType"));
        boolean exposesPolymorphicMapper = Arrays.stream(JacksonSupport.class.getDeclaredMethods())
                .map(method -> method.getName())
                .anyMatch(name -> name.equals("polymorphicMapper"));

        assertThat(restored).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) restored).get("@class")).isEqualTo(Payload.class.getName());
        assertThat(exposesTypeLoadingApi).isFalse();
        assertThat(exposesPolymorphicMapper).isFalse();
    }

    @Test
    void shouldSmartMatchPropertyNamesAndTreatEmptyTemporalStringsAsNull() {

        Payload restored = Jsons.parse("{\"created_at\":\"\",\"STATUS\":\"enabled\"}", Payload.class);

        assertThat(restored.createdAt).isNull();
        assertThat(restored.status).isEqualTo(Status.ENABLED);
    }

    private Payload payload() {

        Payload payload = new Payload();
        payload.amount = new BigDecimal("12.3");
        payload.createdAt = Instant.parse("2026-03-11T04:05:06Z");
        payload.largeId = 9007199254740992L;
        payload.ownerId = 7L;
        payload.status = Status.ENABLED;
        PopulateContextHolder.preload("testUserLoader", Set.of(7L), keys -> Map.of(7L, Map.of("name", "Ada")));
        return payload;
    }

    enum Status {
        ENABLED;

        @Override
        public String toString() {

            return "enabled";
        }
    }

    static class Payload {

        public BigDecimal amount;

        public Instant createdAt;

        public Long largeId;

        public Status status;

        @Sensitive(type = SensitiveType.PHONE)
        public String phone = "13812345678";

        @Sensitive(type = SensitiveType.NAME)
        public String emptyName = "";

        @PopulateField(loader = "testUserLoader")
        public Long ownerId;

        public String optionalNote;
    }
}
