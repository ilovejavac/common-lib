package com.dev.lib.search;

import com.dev.lib.entity.dsl.DslQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseRepositoryAggGuardTest {

    private final TestSearchRepository repository = new TestSearchRepository();

    @Test
    void shouldRejectAggQueryInNormalSearchLoadFlow() {

        TestSearchQuery query = new TestSearchQuery();
        query.agg(SearchAggResult.class)
                .count(TestSearchEntity::getBizId)
                .to(SearchAggResult::setCountValue);

        assertThatThrownBy(() -> repository.load(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agg()")
                .hasMessageContaining("load");
    }

    static class TestSearchRepository extends BaseRepository<TestSearchEntity> {
    }

    static class TestSearchEntity extends SearchEntity {
    }

    static class TestSearchQuery extends DslQuery<TestSearchEntity> {
    }

    static class SearchAggResult {

        private Long countValue;

        public void setCountValue(Long countValue) {

            this.countValue = countValue;
        }
    }
}
