package com.dev.lib.web.excel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * excel 导出
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExport {
    String file() default "export";
}
