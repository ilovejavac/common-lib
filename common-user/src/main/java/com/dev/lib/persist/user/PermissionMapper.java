package com.dev.lib.persist.user;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionMapper extends BaseRepository<PermissionPo> {

    @Getter
    @Setter
    class LoadPermission extends DslQuery<PermissionPo> {

    }
}
