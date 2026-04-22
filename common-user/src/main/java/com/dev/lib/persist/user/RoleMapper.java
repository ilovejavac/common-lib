package com.dev.lib.persist.user;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleMapper extends BaseRepository<RolePo> {

    @Getter
    @Setter
    class LoadRole extends DslQuery<RolePo> {

    }
}
