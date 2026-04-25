package com.dev.lib.jpa.entity.write;

import com.dev.lib.jpa.entity.JpaEntity;

import java.util.List;

public interface RepositoryWritePlugin {

    default int getOrder() {

        return 0;
    }

    boolean supports(RepositoryWriteContext<?> context);

    <T extends JpaEntity, S extends T> S save(RepositoryWriteContext<T> context, S entity);

    <T extends JpaEntity, S extends T> List<S> saveAll(RepositoryWriteContext<T> context, Iterable<S> entities);
}
