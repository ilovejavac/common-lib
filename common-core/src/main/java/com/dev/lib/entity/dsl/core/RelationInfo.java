package com.dev.lib.entity.dsl.core;

import lombok.Getter;

@Getter
public class RelationInfo {
    private final RelationType relationType;
    private final Class<?> targetEntity;
    private final String fieldName;
    private final String joinField;
    private final String mappedBy;

    public RelationInfo(RelationType relationType, Class<?> targetEntity,
                        String fieldName, String joinField, String mappedBy) {
        this.relationType = relationType;
        this.targetEntity = targetEntity;
        this.fieldName = fieldName;
        this.joinField = joinField;
        this.mappedBy = mappedBy;
    }
}