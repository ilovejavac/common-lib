package org.example.commonlib.jpa.write.doris;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;

@Entity
public class DorisWriteEntity extends JpaEntity {

    private String name;

    public DorisWriteEntity() {
    }

    public DorisWriteEntity(String name) {

        this.name = name;
    }

    public String getName() {

        return name;
    }
}
