package com.dev.lib.local.task.message.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotifyType {
    RABBIT,
    HTTP,
    MQ
}
