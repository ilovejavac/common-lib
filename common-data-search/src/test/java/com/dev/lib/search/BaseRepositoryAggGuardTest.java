package com.dev.lib.search;

import com.dev.lib.entity.dsl.DslQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void pageWithoutDslQueryShouldUseDefaultPageable() {

        Page<TestSearchEntity> page = repository.page(null);

        assertThat(page.getContent()).isEmpty();
        assertThat(repository.pageable).isNotNull();
        assertThat(repository.pageable.getOffset()).isZero();
        assertThat(repository.pageable.getPageSize()).isEqualTo(128);
    }

    @Test
    void deleteWithoutBusinessConditionShouldFailBeforeDeletingEverything() {

        assertThatThrownBy(() -> repository.delete((DslQuery<TestSearchEntity>) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("批量删除必须指定业务条件");
    }

    static class TestSearchRepository extends BaseRepository<TestSearchEntity> {

        private Pageable pageable;

        @Override
        public Page<TestSearchEntity> findAll(Query query, Pageable pageable) {

            this.pageable = pageable;
            return new PageImpl<>(List.of(), pageable, 0);
        }
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
