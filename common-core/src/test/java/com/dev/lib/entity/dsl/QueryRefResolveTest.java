package com.dev.lib.entity.dsl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRefResolveTest {

    @Test
    void shouldResolveGetterFieldName() {

        QueryRef<UserAgg, String> ref = UserAgg::getName;
        assertThat(ref.getFieldName()).isEqualTo("name");
    }

    @Test
    void shouldResolveSetterFieldName() {

        QuerySetterRef<UserAgg, String> setter = UserAgg::setName;
        assertThat(setter.getFieldName()).isEqualTo("name");
    }

    static class UserAgg {

        private String name;

        public String getName() {

            return name;
        }

        public void setName(String name) {

            this.name = name;
        }
    }
}
