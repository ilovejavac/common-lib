package com.dev.lib.security;

//import com.dev.lib.security.service.AuthenticateService;

import com.dev.lib.security.util.UserDetailsToUserItemMapper;
import com.dev.lib.web.serialize.PopulateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 用户信息加载器
 * Bean 名称 "userLoader" 对应注解中的 loader 值
 */
@Component("userLoader")
@RequiredArgsConstructor
public class UserLoader implements PopulateLoader<Long, UserItem> {

    //    private final AuthenticateService authenticateService;
    private final UserDetailsToUserItemMapper mapper;

    @Override
    public Map<Long, UserItem> batchLoad(Set<Long> keys) {

        return Map.of();
    }

}