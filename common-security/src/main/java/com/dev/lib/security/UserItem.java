package com.dev.lib.security;

import com.dev.lib.security.util.UserDetails;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@AutoMapper(target = UserDetails.class, convertGenerate = false)
public class UserItem {

    private String email;

    private String phone;

    private String username;

    private String realName;

}