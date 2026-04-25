package com.dev.lib.datalake;

import com.dev.lib.datalake.config.DatalakeProperties;
import com.dev.lib.jpa.multiple.JpaDialect;

public class DorisRepositoryWritePlugin extends AbstractDatalakeRepositoryWritePlugin {

    private final DatalakeProperties properties;

    public DorisRepositoryWritePlugin(DatalakeProperties properties) {

        super(JpaDialect.DORIS, "app.datalake.doris");
        this.properties = properties;
    }

    @Override
    protected boolean isEnabled() {

        return properties.getDoris().isEnabled();
    }

    @Override
    protected boolean hasDatasourceConfiguration(String datasourceName) {

        return !properties.getDoris().getStreamLoadUrls().isEmpty();
    }
}
