package com.dev.lib.datalake;

import com.dev.lib.datalake.config.DatalakeProperties;
import com.dev.lib.jpa.multiple.JpaDialect;

public class HiveRepositoryWritePlugin extends AbstractDatalakeRepositoryWritePlugin {

    private final DatalakeProperties properties;

    public HiveRepositoryWritePlugin(DatalakeProperties properties) {

        super(JpaDialect.HIVE, "app.datalake.hive");
        this.properties = properties;
    }

    @Override
    protected boolean isEnabled() {

        return properties.getHive().isEnabled();
    }

    @Override
    protected boolean hasDatasourceConfiguration(String datasourceName) {

        return !properties.getHive().getUrls().isEmpty();
    }
}
