package com.dev.lib.excel;

import java.lang.annotation.*;

/**
 * 标记方法接收 Excel 上传
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelImport {

    /**
     * 上传文件参数名
     */
    String fileParam() default "file";

}