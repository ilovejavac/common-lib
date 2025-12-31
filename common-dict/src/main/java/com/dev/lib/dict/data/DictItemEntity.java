package com.dev.lib.dict.data;

import com.dev.lib.dict.domain.model.dto.DictItemDTO;
import com.dev.lib.dict.domain.model.valobj.DictItemVO;
import com.dev.lib.dict.serialize.DictItem;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.jpa.entity.JpaEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_dict_item", indexes = {
        @Index(name = "idx_item_code", columnList = "item_code", unique = true),
        @Index(name = "idx_type_id", columnList = "type_id")
})
@AutoMapper(target = DictItemDTO.CreateItem.class)
@AutoMapper(target = DictItemDTO.UpdateItem.class)
@AutoMapper(target = DictItemVO.class)
@AutoMapper(target = DictItem.class)
public class DictItemEntity extends JpaEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(nullable = false)
    private String itemLabel;

    @Column(length = 50)
    private String css;

    @Column(nullable = false)
    private Integer sort = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityStatus status = EntityStatus.ENABLE;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private DictType dictType;

}