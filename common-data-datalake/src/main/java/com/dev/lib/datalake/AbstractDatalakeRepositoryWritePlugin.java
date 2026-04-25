package com.dev.lib.datalake;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.write.RepositoryWriteContext;
import com.dev.lib.jpa.entity.write.RepositoryWritePlugin;
import com.dev.lib.jpa.multiple.JpaDialect;
import org.springframework.core.Ordered;

import java.util.List;

public abstract class AbstractDatalakeRepositoryWritePlugin implements RepositoryWritePlugin {

    private final JpaDialect dialect;

    private final String configurationPrefix;

    protected AbstractDatalakeRepositoryWritePlugin(JpaDialect dialect, String configurationPrefix) {

        this.dialect = dialect;
        this.configurationPrefix = configurationPrefix;
    }

    @Override
    public int getOrder() {

        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public boolean supports(RepositoryWriteContext<?> context) {

        return context != null && isEnabled() && context.logicalDialect() == dialect;
    }

    @Override
    public <T extends JpaEntity, S extends T> S save(RepositoryWriteContext<T> context, S entity) {

        throw notImplemented(context, "save");
    }

    @Override
    public <T extends JpaEntity, S extends T> List<S> saveAll(RepositoryWriteContext<T> context, Iterable<S> entities) {

        throw notImplemented(context, "saveAll");
    }

    protected JpaDialect dialect() {

        return dialect;
    }

    protected boolean isEnabled() {

        return true;
    }

    protected boolean hasDatasourceConfiguration(String datasourceName) {

        return false;
    }

    protected UnsupportedOperationException notImplemented(RepositoryWriteContext<?> context, String operation) {

        String entityName = context == null || context.entityClass() == null
                ? "unknown"
                : context.entityClass().getName();
        String datasourceName = context == null
                ? "unknown"
                : context.datasourceName();
        String datasourcePath = configurationPrefix;
        String configurationState = hasDatasourceConfiguration(datasourceName)
                ? "configured at " + datasourcePath
                : "missing " + datasourcePath;
        return new UnsupportedOperationException(
                "Data lake " + operation + " for dialect " + dialect
                        + " is not implemented; datasource=" + datasourceName
                        + ", entity=" + entityName
                        + ", configuration=" + configurationState
                        + ". Provide a concrete common-data-datalake writer before using this repository."
        );
    }
}
