package com.dev.lib.persist.adapt;

import com.dev.lib.biz.user.repo.IUserQueryRepo;
import com.dev.lib.biz.user.repo.IUserRepo;
import com.dev.lib.persist.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAdapt implements IUserQueryRepo, IUserRepo {

    private final UserMapper userMapper;

    @Override
    public Long register() {

        return 0L;
    }

}
