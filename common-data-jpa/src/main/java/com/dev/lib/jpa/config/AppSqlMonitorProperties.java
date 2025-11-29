package com.dev.lib.jpa.config;

import lombok.Data;

@Data
public class AppSqlMonitorProperties {
    private Boolean enabled = true;
    private Long slowThreshold = 1000L;
    private Boolean logEnabled = true;
    private Boolean saveEnabled = true;
}