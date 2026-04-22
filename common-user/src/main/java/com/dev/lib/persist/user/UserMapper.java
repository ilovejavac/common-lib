package com.dev.lib.persist.user;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseRepository<UserPo> {

    @Getter
    @Setter
    class LoadUser extends DslQuery<UserPo> {

    }

}
