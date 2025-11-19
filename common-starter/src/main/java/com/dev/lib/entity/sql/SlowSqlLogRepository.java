package com.dev.lib.entity.sql;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SlowSqlLogRepository extends JpaRepository<SlowSqlLog, Long> {
}
