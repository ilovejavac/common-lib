package com.dev.lib.web;

import com.dev.lib.web.model.ServerResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页接口
 */
@RestController
public class Home {

    @Value("${spring.application.name}")
    private String application;

    @RequestMapping("/")
    public ServerResponse<String> home() {
        return ServerResponse.success(
                "welcome, here is %s server!".formatted(application)
        );
    }
}
