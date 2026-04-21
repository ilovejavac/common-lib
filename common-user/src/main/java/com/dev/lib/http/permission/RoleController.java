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
public class RoleController {

    // 创建角色
    public ServerResponse<String> createRole() {

        return ServerResponse.success("");
    }

    // 删除角色

    // 修改角色

    // 角色列表

    // 角色详情
}
