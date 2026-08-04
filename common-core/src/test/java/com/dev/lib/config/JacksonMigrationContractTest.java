package com.dev.lib.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.util.Jsons;
import com.dev.lib.web.BaseVO;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonMigrationContractTest {

    @Test
    void shouldExposeJacksonTreeAndGenericTypeApis() {

        Object tree = Jsons.readTree("{\"code\":200}");
        boolean hasJacksonTypeReferenceOverload = Arrays.stream(Jsons.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("parse"))
                .map(Method::getParameterTypes)
                .anyMatch(parameters -> parameters.length == 2 && parameters[1] == TypeReference.class);

        assertThat(tree).isInstanceOf(JsonNode.class);
        assertThat(((JsonNode) tree).get("code").asInt()).isEqualTo(200);
        assertThat(hasJacksonTypeReferenceOverload).isTrue();
    }

    @Test
    void shouldUseJacksonAnnotationsForPublicNamesAndIgnoredQueryState() throws Exception {

        JsonProperty id = BaseVO.class.getDeclaredField("bizId").getAnnotation(JsonProperty.class);
        JsonIgnore externalFields = DslQuery.class.getDeclaredField("externalFields").getAnnotation(JsonIgnore.class);

        assertThat(id).isNotNull();
        assertThat(id.value()).isEqualTo("id");
        assertThat(externalFields).isNotNull();
    }

    @Test
    void shouldNotProvideDynamicTypeLoadingInfrastructure() {

        assertThatThrownBy(() -> Class.forName("com.dev.lib.config.JacksonTypeAllowList"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.dev.lib.config.properties.AppJsonProperties"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
