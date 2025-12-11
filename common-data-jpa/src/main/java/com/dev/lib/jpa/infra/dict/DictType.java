package com.dev.lib.jpa.infra.dict;

import com.dev.lib.dict.domain.model.dto.DictTypeDTO;
import com.dev.lib.dict.domain.model.valobj.DictTypeVO;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.jpa.entity.JpaEntity;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "sys_dict_type", indexes = {
        @Index(name = "idx_type_code", columnList = "type_code", unique = true)
})
@AutoMappers({
        @AutoMapper(target = DictTypeDTO.CreateType.class),
        @AutoMapper(target = DictTypeDTO.UpdateType.class),
        @AutoMapper(target = DictTypeVO.class),
})
public class DictType extends JpaEntity {

    @Comment("字典类型编码")
    @Column(nullable = false, unique = true, length = 50)
    private String typeCode;

    @Comment("字典类型名称")
    @Column(nullable = false, length = 100)
    private String typeName;

    @Comment("排序")
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