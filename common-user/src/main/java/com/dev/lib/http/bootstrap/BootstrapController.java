package com.dev.lib.http.bootstrap;

import com.dev.lib.biz.bootstrap.model.BootstrapCmd;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BootstrapController {

    // 初始化系统
    @PostMapping("/api/bootstrap/init")
    public ServerResponse<Boolean> initSystem(@Validated @RequestBody BootstrapCmd.BootstrapSystem cmd) {

        return ServerResponse.ok();
    }

}
