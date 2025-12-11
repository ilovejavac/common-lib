package com.dev.lib.search;

import com.dev.lib.entity.CoreEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class SearchEntity extends CoreEntity {

//    private Long id;

    private String bizId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long creatorId;

    private Long modifierId;

    private Boolean deleted;

    public boolean isNew() {

        return bizId == null || createdAt == null;
    }

}
