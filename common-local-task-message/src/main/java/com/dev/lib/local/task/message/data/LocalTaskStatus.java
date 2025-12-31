package com.dev.lib.local.task.message.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocalTaskStatus {
    PENDING,     // 待处理
    PROCESSING,  // 处理中（防并发）
    SUCCESS,     // 成功
    FAILED,      // 最终失败（放弃重试）
}
