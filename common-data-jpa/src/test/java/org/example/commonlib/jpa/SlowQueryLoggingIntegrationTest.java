package org.example.commonlib.jpa;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlowQueryLoggingIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(SlowQueryLoggingApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:slow_query_logging;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=slow-query-logging-test"
            );

    @Test
    void shouldRegisterAfterQuerySlowListenerByDefaultAndAllowDisable() {

        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(QueryExecutionListener.class))
                    .hasSize(1)
                    .containsKey("finalSlowQueryLoggingListener");
        });

        contextRunner.withPropertyValues(
                        "app.jpa.slow-query.enabled=false"
                )
                .run(context -> {
                    assertThat(context.getBeansOfType(QueryExecutionListener.class)).isEmpty();
                });

        contextRunner.withPropertyValues(
                        "decorator.datasource.enabled=false"
                )
                .run(context -> {
                    assertThat(context.getBeansOfType(QueryExecutionListener.class)).isEmpty();
                });
    }

    @Test
    void shouldLogFinalElapsedTimeOnlyAfterQueryCompletes() {

        String loggerName = "org.example.commonlib.jpa.slow-query-test";
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            contextRunner.withPropertyValues(
                            "app.jpa.slow-query.threshold=1s",
                            "app.jpa.slow-query.logger-name=" + loggerName
                    )
                    .run(context -> {
                        QueryExecutionListener listener = context.getBean("finalSlowQueryLoggingListener", QueryExecutionListener.class);

                        QueryInfo queryInfo = new QueryInfo("select * from slow_query_thing where id = ?");
                        ExecutionInfo fast = executionInfo(999L);
                        ExecutionInfo slow = executionInfo(1500L);

                        listener.afterQuery(fast, List.of(queryInfo));
                        listener.afterQuery(slow, List.of(queryInfo));

                        assertThat(appender.list).hasSize(1);
                        String message = appender.list.getFirst().getFormattedMessage();
                        assertThat(message).contains("Time:1500");
                        assertThat(message).contains("Query:[\"select * from slow_query_thing where id = ?\"]");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static ExecutionInfo executionInfo(long elapsedTime) {

        ExecutionInfo executionInfo = new ExecutionInfo();
        executionInfo.setElapsedTime(elapsedTime);
        executionInfo.setSuccess(true);
        executionInfo.setBatch(false);
        executionInfo.setBatchSize(0);
        executionInfo.setStatementType(StatementType.PREPARED);
        executionInfo.setDataSourceName("primary");
        executionInfo.setConnectionId("1");
        return executionInfo;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class SlowQueryLoggingApplication {
    }
}

@Entity
class SlowQueryThing extends JpaEntity {
}

interface SlowQueryThingRepo extends BaseRepository<SlowQueryThing> {
}
