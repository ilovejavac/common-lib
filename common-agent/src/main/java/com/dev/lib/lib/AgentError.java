package com.dev.lib.lib;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentError implements CodeEnums {

    MESSAGE_FULL(801201, "消息队列已满")
    ;
    private final Integer code;
    private final String message;
}
