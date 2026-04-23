package com.dev.lib.config;

import com.dev.lib.biz.user.repo.IUserQueryRepo;
import com.dev.lib.biz.permission.service.permission.SyncPermissionUseCase;
import com.dev.lib.security.model.EndpointPermission;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.util.UserDetails;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class AuthModule implements AuthenticateService {

    @Resource
    private SyncPermissionUseCase syncPermissionUseCase;

    @Resource
    private IUserQueryRepo userQueryRepo;

    @Override
    public UserDetails loadUserById(Long id) {

        return null;
    }

    @Override
    public Collection<UserDetails> batchLoadUserByIds(Set<Long> ids) {

        return List.of();
    }

    @Override
    public void registerPermissions(List<EndpointPermission> permissions) {

        syncPermissionUseCase.execute(permissions);
    }

}
