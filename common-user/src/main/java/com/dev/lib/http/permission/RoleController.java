package com.dev.lib.http.permission;

import com.dev.lib.biz.permission.repo.IRoleQueryRepo;
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

}
