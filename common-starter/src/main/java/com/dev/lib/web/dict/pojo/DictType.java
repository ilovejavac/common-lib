package com.dev.lib.web.dict.pojo;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.EntityStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "sys_dict_type", indexes = {
        @Index(name = "idx_type_code", columnList = "type_code", unique = true),
        @Index(columnList = "type_id", unique = true)
})
public class DictType extends BaseEntity {

    @Column(length = 12)
    private String typeId;

    @Comment("字典类型编码")
    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode;

    @Comment("字典类型名称")
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Comment("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Column(nullable = false)
    private EntityStatus status;

    @OneToMany(mappedBy = "dictType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DictItemEntity> items = new ArrayList<>();
}