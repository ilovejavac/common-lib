package com.dev.lib.schedule.data;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sys_scheduled_task")
@Data
public class ScheduledTask extends JpaEntity {

    private String  taskName;        // 任务名称

    private String  beanName;        // Bean名称

    private String  methodName;      // 方法名

    private String  params;          // 参数(JSON)

    private String  cronExpression;  // Cron表达式

    private String  description;     // 描述

    private Boolean enabled;        // 是否启用

    private String  lastExecuteTime; // 最后执行时间

    private String  lastExecuteStatus; // 最后执行状态

    private String  lastExecuteMessage; // 最后执行消息

}