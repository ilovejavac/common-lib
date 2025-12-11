package com.dev.lib.config.properties;

import lombok.Data;

import java.util.Set;

@Data
public class AppDubboProperties {

    private Set<String> scanPackages;

}
