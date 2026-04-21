package com.dev.lib.http.permission;

import com.dev.lib.security.service.annotation.RequireRole;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RequireRole("admin")
@Slf4j
@RestController
@RequiredArgsConstructor
public class PermissionController {

    // 修改权限
    public ServerResponse<String> updatePermission() {
        return ServerResponse.ok();
    }

    // 权限列表
    public ServerResponse<String> listPermission() {

        return ServerResponse.ok();
    }

    // 权限详情
    public ServerResponse<String> getPermission() {

        return ServerResponse.ok();
    }
}
