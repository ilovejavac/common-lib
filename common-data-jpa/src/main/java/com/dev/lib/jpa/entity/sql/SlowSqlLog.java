package com.dev.lib.jpa.entity.sql;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sys_slow_sql_log")
@Data

public class SlowSqlLog extends JpaEntity {

    @Column(columnDefinition = "TEXT")
    private String sql;             // SQL语句

    private Long executeTime;       // 执行时间(ms)
    private String parameters;      // 参数
    private String method;          // 调用方法
    private String stackTrace;      // 堆栈信息
    private String username;        // 操作人
}