package com.dev.lib.web.serialize.userinfo;

import com.dev.lib.security.AuthenticateService;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.security.util.UserDetailsToUserLoader$UserItemMapper;
import com.dev.lib.web.serialize.PopulateLoader;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户信息加载器
 * Bean 名称 "userLoader" 对应注解中的 loader 值
 */
@Component("userLoader")
@RequiredArgsConstructor
public class UserLoader implements PopulateLoader<Long, UserLoader.UserItem> {

    private final AuthenticateService authenticateService;
    private final UserDetailsToUserLoader$UserItemMapper mapper;

    @Override
    public Map<Long, UserItem> batchLoad(Set<Long> ids) {
       return authenticateService.batchLoadUserByIds(ids)
                .stream()
                .collect(Collectors.toMap(UserDetails::getId, mapper::convert));
    }

    @Override
    public Class<Long> keyType() {
        return Long.class;
    }

    @Data
    public static class UserItem {
        private String email;
        private String phone;
        private String username;
        private String realName;
    }
}