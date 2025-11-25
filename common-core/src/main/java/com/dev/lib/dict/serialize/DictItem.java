package com.dev.lib.dict.serialize;

import io.github.linpeilie.annotations.AutoMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItem {
    @AutoMapping(target = "itemCode")
    private String code;
    @AutoMapping(target = "itemLabel")
    private String label;
    private String css;
}