package com.dev.lib.biz.bootstrap.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class BootstrapCmd {

    private BootstrapCmd() {

    }

    @Getter
    @Setter
    public static class BootstrapSystem {

        /**
         * 登录凭证
         */
        @NotBlank(message = "登录凭证不能为空")
        private String mail;

        private String password;

    }

}
