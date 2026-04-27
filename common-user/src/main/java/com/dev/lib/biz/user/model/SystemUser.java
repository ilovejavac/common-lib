package com.dev.lib.biz.user.model;

import com.dev.lib.biz.permission.model.Role;
import com.dev.lib.domain.AggregateRoot;
import com.dev.lib.security.model.UserStatus;
import com.dev.lib.security.model.UserType;
import lombok.Data;

import java.util.Set;

@Data
public class SystemUser extends AggregateRoot {

    private String username;

    private String email;

    private String phone;

    private Set<Role> roles;

    private Dept dept;

    private Set<Dept> depts;

    // ===== 用户状态  =====
    private String realName;          // 真实姓名(用于日志/审计)

    private UserType userType;          // 用户类型: EMPLOYEE, ADMIN, SYSTEM 等

    private UserStatus status;           // 用户状态

}
