package com.dev.lib.web.dict.data;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.EntityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "sys_dict_item", indexes = {
        @Index(name = "idx_item_code", columnList = "item_code", unique = true),
        @Index(name = "idx_type_id", columnList = "type_id")
})
public class DictItemEntity extends BaseEntity {
    @Comment("字典项编码")
    @Column(name = "item_code", nullable = false, unique = true, length = 50)
    private String itemCode;

    @Comment("字典项标签")
    @Column(name = "item_label", nullable = false, length = 100)
    private String itemLabel;

    @Comment("样式类名")
    @Column(length = 50)
    private String css;

    @Comment("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityStatus status = EntityStatus.ENABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private DictType dictType;
}