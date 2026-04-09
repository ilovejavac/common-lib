package com.dev.lib.jpa.config;

import com.dev.lib.jpa.entity.JpaEntity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动为所有 JpaEntity 子类创建通用索引：
 * <ul>
 *   <li>idx_{table}_deleted_updated_at (deleted, updated_at) — 覆盖按更新时间筛选/排序</li>
 * </ul>
 * biz_id 已有 unique 约束，无需额外索引。
 * deleted + id 分页走主键索引扫描即可，无需额外索引。
 */
public class BaseEntityIndexIntegrator implements Integrator {

    private static final String[][] INDEX_COLUMNS = {
            {"deleted", "updated_at"},
    };

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {

        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            if (!JpaEntity.class.isAssignableFrom(persistentClass.getMappedClass())) {
                continue;
            }

            Table table = persistentClass.getTable();

            for (String[] columnNames : INDEX_COLUMNS) {
                String indexName = buildIndexName(table.getName(), columnNames);

                if (table.getIndex(indexName) != null) {
                    continue;
                }

                List<Column> columns = resolveColumns(table, columnNames);
                if (columns.isEmpty()) {
                    continue;
                }

                var index = table.getOrCreateIndex(indexName);
                columns.forEach(index::addColumn);
            }
        }
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        // no-op
    }

    private static String buildIndexName(String tableName, String[] columnNames) {
        return "idx_" + tableName + "_" + String.join("_", columnNames);
    }

    private static List<Column> resolveColumns(Table table, String[] columnNames) {
        List<Column> columns = new ArrayList<>(columnNames.length);
        for (String name : columnNames) {
            Column column = table.getColumn(new Column(name));
            if (column == null) {
                return List.of();
            }
            columns.add(column);
        }
        return columns;
    }
}
