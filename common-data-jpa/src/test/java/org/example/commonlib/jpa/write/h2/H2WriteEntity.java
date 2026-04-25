package org.example.commonlib.jpa.write.h2;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;

@Entity
public class H2WriteEntity extends JpaEntity {

    private String name;

    public H2WriteEntity() {
    }

    public H2WriteEntity(String name) {

        this.name = name;
    }

    public String getName() {

        return name;
    }
}
