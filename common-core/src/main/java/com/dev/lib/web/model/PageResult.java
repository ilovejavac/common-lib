package com.dev.lib.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
final class PageResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer page;

    private Integer size;

    private Long total;

    private Boolean hasNext;

}
