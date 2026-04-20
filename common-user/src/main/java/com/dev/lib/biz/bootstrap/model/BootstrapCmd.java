package com.dev.lib.biz.bootstrap.model;

import lombok.Getter;
import lombok.Setter;

public class BootstrapCmd {

    private BootstrapCmd() {
    }

    @Getter
    @Setter
    public static class BootstrapSystem {
        private String email;
        private String password;
    }
}
