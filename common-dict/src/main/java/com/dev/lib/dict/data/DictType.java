package com.dev.lib.dict.data;

import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.dict.domain.model.valobj.DictTypeVO;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.jpa.entity.JpaEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "sys_dict_type", indexes = {
        @Index(name = "idx_type_code", columnList = "type_code", unique = true)
})
@AutoMapper(target = DictTypeDTO.CreateType.class, convertGenerate = false)
@AutoMapper(target = DictTypeDTO.UpdateType.class, convertGenerate = false)
@AutoMapper(target = DictTypeVO.class, reverseConvertGenerate = false)
public class DictType extends JpaEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String typeCode;

    @Column(nullable = false, length = 100)
    private String typeName;

    @Column(nullable = false)
    private Integer sort = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityStatus status;

    @OneToMany(mappedBy = "dictType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DictItemEntity> items = new ArrayList<>();

    public void addItem(DictItemEntity item) {

        item.setDictType(this);
        items.add(item);
    }

    public void removeItem(DictItemEntity item) {

        item.setDictType(null);
        items.remove(item);
    }

}