package com.dev.lib.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
class PageResult implements Serializable {

    private Integer page;

    private Integer size;

    private Long total;

    private Boolean hasNext;

}
