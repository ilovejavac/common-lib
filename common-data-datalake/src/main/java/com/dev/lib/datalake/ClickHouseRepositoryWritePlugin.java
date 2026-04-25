package com.dev.lib.datalake;

import com.dev.lib.datalake.config.DatalakeProperties;
import com.dev.lib.jpa.multiple.JpaDialect;

public class ClickHouseRepositoryWritePlugin extends AbstractDatalakeRepositoryWritePlugin {

    private final DatalakeProperties properties;

    public ClickHouseRepositoryWritePlugin(DatalakeProperties properties) {

        super(JpaDialect.CLICKHOUSE, "app.datalake.clickhouse");
        this.properties = properties;
    }

    @Override
    protected boolean isEnabled() {

        return properties.getClickhouse().isEnabled();
    }

    @Override
    protected boolean hasDatasourceConfiguration(String datasourceName) {

        return !properties.getClickhouse().getUrls().isEmpty();
    }
}
