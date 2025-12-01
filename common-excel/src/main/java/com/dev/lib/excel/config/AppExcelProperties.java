package com.dev.lib.excel.config;

import lombok.Data;

@Data
public class AppExcelProperties {
    private ExcelLoadAction load = ExcelLoadAction.DOWNLOAD;
    private String excelLoadHeader = "X-Excel-load";
}
