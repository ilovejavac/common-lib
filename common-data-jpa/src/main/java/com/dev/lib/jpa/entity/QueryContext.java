package com.dev.lib.jpa.entity;

import jakarta.persistence.LockModeType;

public final class QueryContext {

    public enum DeletedFilter {
        EXCLUDE_DELETED,
        INCLUDE_DELETED,
        ONLY_DELETED
    }

    private LockModeType lockMode;

    private DeletedFilter deletedFilter = DeletedFilter.EXCLUDE_DELETED;

    public LockModeType getLockMode() {

        return lockMode;
    }

    public DeletedFilter getDeletedFilter() {

        return deletedFilter;
    }

    public boolean hasLock() {

        return lockMode != null;
    }

    public QueryContext lockForUpdate() {

        lockMode = LockModeType.PESSIMISTIC_WRITE;
        return this;
    }

    public QueryContext lockForShare() {

        lockMode = LockModeType.PESSIMISTIC_READ;
        return this;
    }

    public QueryContext withDeleted() {

        deletedFilter = DeletedFilter.INCLUDE_DELETED;
        return this;
    }

    public QueryContext onlyDeleted() {

        deletedFilter = DeletedFilter.ONLY_DELETED;
        return this;
    }
}
