package com.dev.lib.excel;

import com.dev.lib.exceptions.BizException;

public class ExcelException extends BizException {

    public ExcelException(String message) {

        super(
                50030,
                message
        );
    }

}
