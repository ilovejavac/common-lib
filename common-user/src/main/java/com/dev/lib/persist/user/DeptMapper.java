package com.dev.lib.persist.user;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeptMapper extends BaseRepository<DeptPo> {

    class LoadDept extends DslQuery<DeptPo> {

    }
}
