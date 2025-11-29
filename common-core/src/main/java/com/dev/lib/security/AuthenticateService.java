package com.dev.lib.security;

import com.dev.lib.security.util.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AuthenticateService {
    /**
     * 加载用户信息
     */
    UserDetails loadUserById(Long id);

    Collection<UserDetails> batchLoadUserByIds(Set<Long> ids);

    void registerPermissions(List<EndpointPermission> permissions);
}
