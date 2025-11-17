package com.dev.lib.web.dict.model.valobj;

import com.dev.lib.entity.EntityStatus;
import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Data
public class DictTypeVO {
    private String bizId;

    private String typeCode;

    private String typeName;

    private Integer sort;

    private EntityStatus status;

    private List<DictItemVO> items = new ArrayList<>();
}
