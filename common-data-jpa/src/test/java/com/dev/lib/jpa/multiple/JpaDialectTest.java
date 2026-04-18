package com.dev.lib.jpa.multiple;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaDialectTest {

    @Test
    void shouldResolveCommonDialectMappings() {

        assertThat(JpaDialect.SQLITE.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
        assertThat(JpaDialect.POSTGRESQL.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
        assertThat(JpaDialect.MYSQL.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.dialect.MySQLDialect");
        assertThat(JpaDialect.DORIS.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.dialect.MySQLDialect");
        assertThat(JpaDialect.GAUSSDB.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.community.dialect.GaussDBDialect");
        assertThat(JpaDialect.OPENGAUSS.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.community.dialect.GaussDBDialect");
        assertThat(JpaDialect.DM.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.dialect.DmDialect");
        assertThat(JpaDialect.KINGBASE.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.dialect.Kingbase8Dialect");
        assertThat(JpaDialect.HDB.resolveDatabasePlatform("", null))
                .isEqualTo("org.hibernate.dialect.HANADialect");
    }

    @Test
    void shouldResolveAutoFromCustomThenGlobal() {

        assertThat(JpaDialect.AUTO.resolveDatabasePlatform("org.hibernate.community.dialect.SQLiteDialect", "org.hibernate.dialect.H2Dialect"))
                .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
        assertThat(JpaDialect.AUTO.resolveDatabasePlatform("", "org.hibernate.dialect.H2Dialect"))
                .isEqualTo("org.hibernate.dialect.H2Dialect");
    }

    @Test
    void shouldRequireCustomPlatformForUnsupportedDialects() {

        assertThatThrownBy(() -> JpaDialect.CLICKHOUSE.resolveDatabasePlatform("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLICKHOUSE");

        assertThatThrownBy(() -> JpaDialect.HIVE.resolveDatabasePlatform("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HIVE");

        assertThat(JpaDialect.CLICKHOUSE.resolveDatabasePlatform("com.example.ClickHouseDialect", null))
                .isEqualTo("com.example.ClickHouseDialect");
    }

    @Test
    void shouldExposeMappedDialectClassesOnClasspath() throws Exception {

        assertThat(Class.forName(JpaDialect.SQLITE.resolveDatabasePlatform("", null))).isNotNull();
        assertThat(Class.forName(JpaDialect.DM.resolveDatabasePlatform("", null))).isNotNull();
        assertThat(Class.forName(JpaDialect.KINGBASE.resolveDatabasePlatform("", null))).isNotNull();
        assertThat(Class.forName(JpaDialect.HDB.resolveDatabasePlatform("", null))).isNotNull();
    }
}
