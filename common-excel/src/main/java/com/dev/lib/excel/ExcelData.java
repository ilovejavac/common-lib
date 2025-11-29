package com.dev.lib.excel;

import java.lang.annotation.*;

/**
 * 标记参数接收解析后的 Excel 数据
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelData {
}