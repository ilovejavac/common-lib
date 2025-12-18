package com.dev.lib.cloud.config;

import lombok.Data;

@Data
public class AppCloudProperties {

    private String host;

    private String port;

    private String namespace;

    private String fileExtension;

    private String group;

    private String username;

    private String password;

}
