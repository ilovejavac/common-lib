package com.dev.lib.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 */
@RestController
public class Healthz {
    @RequestMapping("/healthz")
    public String healthz() {
        return "response";
    }
}
