package com.dev.lib.biz.bootstrap.model;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OauthType implements CodeEnums {
    google(0, "谷歌"),
    microsoft(1, "微软"),
    facebook(2, "脸书"),

    wechat(10, "微信"),
    feishu(11, "飞书"),
    dingding(12, ""),

    mail(100, "邮箱"),
    mobile(101, "电话号码"),

    ;

    private final Integer code;

    private final String  message;
}
