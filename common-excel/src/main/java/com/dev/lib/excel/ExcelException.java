package com.dev.lib.excel;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.model.StandardErrorCodes;

public class ExcelException extends BizException {

    public ExcelException(String message) {

        super(
                StandardErrorCodes.REQUEST_BODY_INVALID,
                message
        );
    }

}
