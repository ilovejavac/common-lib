package com.dev.lib.aksk.trigger;

import com.dev.lib.aksk.domain.model.dto.AkskDTO;
import com.dev.lib.aksk.domain.model.vo.AkskVO;
import com.dev.lib.aksk.domain.service.AkskService;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class AkskAdminController {

    private final AkskService service;

    @PostMapping("/admin/aksk/create_aksk")
    public ServerResponse<AkskVO.CreateResult> create(@Valid @RequestBody AkskDTO.Create cmd) {

        return ServerResponse.success(service.create(cmd));
    }

    @PostMapping("/admin/aksk/update_aksk")
    public ServerResponse<Void> update(@Valid @RequestBody AkskDTO.Update cmd) {

        service.update(cmd);
        return ServerResponse.ok();
    }

    @PostMapping("/admin/aksk/delete_aksk/{id}")
    public ServerResponse<Void> delete(@NotBlank @PathVariable String id) {

        service.delete(id);
        return ServerResponse.ok();
    }

    @PostMapping("/admin/aksk/enable_aksk/{id}")
    public ServerResponse<Void> enable(@NotBlank @PathVariable String id) {

        service.enable(id);
        return ServerResponse.ok();
    }

    @PostMapping("/admin/aksk/disable_aksk/{id}")
    public ServerResponse<Void> disable(@NotBlank @PathVariable String id) {

        service.disable(id);
        return ServerResponse.ok();
    }

    @PostMapping("/admin/aksk/reset_secret_aksk/{id}")
    public ServerResponse<AkskVO.ResetSecretResult> resetSecret(@NotBlank @PathVariable String id) {

        return ServerResponse.success(service.resetSecret(id));
    }

    @PostMapping("/admin/aksk/query_list_aksk")
    public ServerResponse<List<AkskVO.ListItem>> queryList(
            @Valid @RequestBody QueryRequest<AkskDTO.Query> request
    ) {

        return service.page(request);
    }

    @GetMapping("/admin/aksk/detail_aksk/{id}")
    public ServerResponse<AkskVO.Detail> detail(@NotBlank @PathVariable String id) {

        return ServerResponse.success(service.detail(id));
    }
}
