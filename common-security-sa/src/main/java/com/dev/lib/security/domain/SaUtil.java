package com.dev.lib.security.domain;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.UserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaUtil implements TokenService {
    private final AuthenticateService authenticateService;

    @Override
    public String generateToken(UserDetails userDetails) {
        StpUtil.login(userDetails.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        SaSession session = StpUtil.getTokenSession();
        session.set("userId", userDetails.getId());
        session.set("username", userDetails.getUsername());
        session.set("permissions", userDetails.getPermissions());
        session.set("roles", userDetails.getRoles());

        return tokenInfo.tokenValue;
    }

    @Override
    public UserDetails parseToken(String token) {
        SaSession session = StpUtil.getTokenSession();
        return new UserDetails()
                .setId(session.get("userId", 0L))
                .setUsername(session.get("username", ""))
                .setPermissions(session.get("permissions", new ArrayList<>()))
                .setRoles(session.get("roles", new ArrayList<>()))
                .setTokenId(token);
    }
}
