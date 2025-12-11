package com.dev.lib.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class PageResult {

    private Integer page;

    private Integer size;

    private Long    total;

    private Boolean hasNext;

}
