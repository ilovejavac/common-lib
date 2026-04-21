package com.dev.lib.http.user;

import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SystemDeptController {

    // 创建部门
    public ServerResponse<String> createDept() {

        return ServerResponse.success("");
    }

    // 删除部门
    public ServerResponse<Boolean> removeDept() {

        return ServerResponse.success(true);
    }

    // 修改部门
    public ServerResponse<String> updateDept() {

        return ServerResponse.ok();
    }

    // 部门列表
    public ServerResponse<String> listDept() {

        return ServerResponse.ok();
    }

    // 部门详情
    public ServerResponse<String> getDept() {

        return ServerResponse.ok();
    }
}
