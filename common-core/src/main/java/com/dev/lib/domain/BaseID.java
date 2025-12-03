package com.dev.lib.domain;

import com.dev.lib.entity.id.IDWorker;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class BaseID {
    private final Long id;

    protected BaseID() {
        id = IDWorker.nextID();
    }
}