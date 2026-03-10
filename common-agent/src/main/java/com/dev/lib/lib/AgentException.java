package com.dev.lib.lib;

import com.dev.lib.exceptions.BizException;

public class AgentException extends BizException {

    public AgentException(AgentError error) {

        super(error.getCode(), error.getMessage());
    }

}
