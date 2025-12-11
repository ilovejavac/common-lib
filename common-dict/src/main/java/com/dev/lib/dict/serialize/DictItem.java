package com.dev.lib.dict.serialize;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItem {

    @JsonProperty("code")
    private String itemCode;

    @JsonProperty("label")
    private String itemLabel;

    private String css;

}