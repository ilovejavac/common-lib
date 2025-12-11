package com.dev.lib.config.properties;

import lombok.Data;

@Data
public class PaginationProperties {

    private int defaultSize     = 20;

    private int maxSize         = 100;

    private int maxTotalRecords = 10000;

}