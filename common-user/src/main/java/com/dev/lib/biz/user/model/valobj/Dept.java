package com.dev.lib.biz.user.model.valobj;

import com.dev.lib.domain.AggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Dept extends AggregateRoot {

    private String name;

    private List<Dept> subdepts;
}
