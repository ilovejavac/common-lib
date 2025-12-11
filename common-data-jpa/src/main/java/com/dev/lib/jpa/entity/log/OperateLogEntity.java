package com.dev.lib.jpa.entity.log;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_operate_log")
@Data

public class OperateLogEntity extends JpaEntity {

    private String        module;

    private String        type;

    private String        description;

    private String        method;          // 方法签名

    private String        requestParams;   // 请求参数

    private String        result;          // 返回值

    private String        ip;

    private String        userAgent;

    private String        operator;

    private Integer       costTime;       // 耗时(ms)

    private LocalDateTime operateTime;

    private Boolean       success;

    private String        errorMsg;

    private Long          deptId;

}