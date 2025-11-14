package com.dev.lib.web.dict;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItem {
    private String code;
    private String label;
    private String css;
}