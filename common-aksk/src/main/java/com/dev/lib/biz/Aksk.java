package com.dev.lib.biz;

import com.dev.lib.domain.AggregateRoot;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Aksk extends AggregateRoot {

    private String ak;

    private String sk;

    public boolean valid(String payload, String signature) {

        return true;
    }

}
