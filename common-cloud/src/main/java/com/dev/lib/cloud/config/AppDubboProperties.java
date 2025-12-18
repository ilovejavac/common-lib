package com.dev.lib.cloud.config;

import lombok.Data;

import java.util.Set;

@Data
public class AppDubboProperties {

    private Set<String> scanPackages;

}
