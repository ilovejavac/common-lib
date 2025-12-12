package com.dev.lib.jpa.entity;

import jakarta.persistence.LockModeType;
import lombok.Data;

@Data
public class QueryContext {

    private LockModeType  lockMode;

    private DeletedFilter deletedFilter = DeletedFilter.EXCLUDE_DELETED;

    public enum DeletedFilter {
        EXCLUDE_DELETED,  // 排除已删除（默认）
        INCLUDE_DELETED,  // 包含已删除
        ONLY_DELETED      // 只查已删除
    }

    public boolean hasLock() {

        return lockMode != null;
    }

    public QueryContext lockForUpdate() {

        this.lockMode = LockModeType.PESSIMISTIC_WRITE;
        return this;
    }

    public QueryContext lockForShare() {

        this.lockMode = LockModeType.PESSIMISTIC_READ;
        return this;
    }

    public QueryContext withDeleted() {

        this.deletedFilter = DeletedFilter.INCLUDE_DELETED;
        return this;
    }

    public QueryContext onlyDeleted() {

        this.deletedFilter = DeletedFilter.ONLY_DELETED;
        return this;
    }

}
