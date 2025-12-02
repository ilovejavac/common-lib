package com.dev.lib.local.task.message.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LocalTaskConfigProperties {

    private List<TaskGroupConfig> groupConfigs;

    @Data
    public static class TaskGroupConfig {
        private String group = "default";
        private List<Integer> houseNumbers = new ArrayList<>();
        private String cron;
        private Long fixedDelayMs = 0L;
        private Integer limit = 100;
    }
}
