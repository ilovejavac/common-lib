package com.dev.lib.jpa.infra.dict;

import com.dev.lib.dict.domain.model.dto.DictItemDTO;
import com.dev.lib.dict.domain.model.valobj.DictItemVO;
import com.dev.lib.dict.serialize.DictItem;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.jpa.entity.JpaEntity;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "sys_dict_item", indexes = {
        @Index(name = "idx_item_code", columnList = "item_code", unique = true),
        @Index(name = "idx_type_id", columnList = "type_id")
})
@AutoMappers({
        @AutoMapper(target = DictItemDTO.CreateItem.class),
        @AutoMapper(target = DictItemDTO.UpdateItem.class),
        @AutoMapper(target = DictItemVO.class),
        @AutoMapper(target = DictItem.class),
})
public class DictItemEntity extends JpaEntity {
    @Comment("字典项编码")
    @Column(nullable = false, unique = true, length = 50)
    private String itemCode;

    @Comment("字典项标签")
    @Column(nullable = false)
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

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private DictType dictType;
}