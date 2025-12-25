package com.dev.lib.jpa.entity.log;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_operate_log")
@Data
public class OperateLogEntity extends JpaEntity {

    private String module;

    private String type;

    private String description;

    private String method;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestParams;   // 请求参数

    @Lob
    @Column(columnDefinition = "TEXT")
    private String result;          // 返回值

    private String ip;

    private String userAgent;

    private String operator;

    private Integer costTime;

    private LocalDateTime operateTime;

    private Boolean success;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMsg;        // 错误信息/堆栈

    private Long deptId;

}
