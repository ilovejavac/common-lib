package com.dev.lib.excel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * excel 导入
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelImport {
    String value() default "file";
}
