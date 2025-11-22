package com.dev.lib.web.dict;

import com.dev.lib.web.dict.data.DictItemEntity;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = DictItemEntity.class)
public class DictItem {
    @AutoMapping(target = "itemCode")
    private String code;
    @AutoMapping(target = "itemLabel")
    private String label;
    private String css;
}