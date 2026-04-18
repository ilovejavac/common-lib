package com.dev.lib.jpa.multiple;

import java.util.LinkedHashSet;
import java.util.Set;

public class JpaManagedDatasourceGroup {

    private final Set<String> datasourceBeanNames;

    public JpaManagedDatasourceGroup(Set<String> datasourceBeanNames) {

        this.datasourceBeanNames = Set.copyOf(new LinkedHashSet<>(datasourceBeanNames));
    }

    public Set<String> getDatasourceBeanNames() {

        return datasourceBeanNames;
    }
}
