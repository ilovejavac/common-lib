package com.dev.lib.dict.serialize;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItem {

    @JSONField(name = "code")
    private String itemCode;

    @JSONField(name = "label")
    private String itemLabel;

    private String css;

}
