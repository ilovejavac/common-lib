package com.dev.lib.domain;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class BaseID {

    private final Long   id;

    private final String bizId;

    protected BaseID() {

        id = IDWorker.nextID();
        bizId = IntEncoder.encode36(id);
    }

}