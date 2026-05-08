package com.dev.lib.testsupport.repository;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Table;

public class CommonLibNestedLedger {

    private CommonLibNestedLedger() {

    }

    @jakarta.persistence.Entity
    @Table(name = "common_lib_nested_ledger")
    public static class Entity extends JpaEntity {

        private String name;

        public String getName() {

            return name;
        }

        public void setName(String name) {

            this.name = name;
        }
    }

    public interface Mapper extends BaseRepository<Entity> {
    }
}
