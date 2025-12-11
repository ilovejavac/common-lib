package com.dev.lib.excel;

import java.lang.annotation.*;

/**
 * 标记方法返回值为 Excel 导出
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelExport {

    /**
     * 文件名模板，支持占位符：
     * ${date} - yyyyMMdd
     * ${datetime} - yyyyMMddHHmmss
     * ${timestamp} - 毫秒时间戳
     */
    String fileName() default "export_${datetime}";

    /**
     * Sheet 名称
     */
    String sheetName() default "Sheet1";

}