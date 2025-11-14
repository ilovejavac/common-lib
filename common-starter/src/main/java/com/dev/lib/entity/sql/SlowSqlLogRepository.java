package com.dev.lib.entity.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface SlowSqlLogRepository extends JpaRepository<SlowSqlLog, Long> {
}
