package com.dev.lib.jpa.entity.insert;

import lombok.Data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Data
public class EntityMeta {

    private final String insertSql;

    private final List<FieldMeta> fields;

    public void setParameters(PreparedStatement ps, Object entity) throws SQLException {

        int idx = 1;
        for (FieldMeta field : fields) {
            ps.setObject(idx++, field.getValue(entity), field.getSqlType());
        }
    }

}