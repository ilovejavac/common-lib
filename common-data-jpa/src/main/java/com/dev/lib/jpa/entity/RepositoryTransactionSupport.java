package com.dev.lib.jpa.entity;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

final class RepositoryTransactionSupport {

    private RepositoryTransactionSupport() {
    }

    static void run(BaseRepositoryImpl<?> repository, Runnable action) {

        call(repository, () -> {
            action.run();
            return null;
        });
    }

    static <R> R call(BaseRepositoryImpl<?> repository, Supplier<R> action) {

        if (TransactionSynchronizationManager.hasResource(repository.getEntityManagerFactory())) {
            return action.get();
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("当前事务未绑定到此 Repository 的 EntityManagerFactory，请使用对应的事务管理器");
        }
        TransactionTemplate template = new TransactionTemplate(
                new JpaTransactionManager(repository.getEntityManagerFactory())
        );
        return template.execute(status -> action.get());
    }
}
