package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.model.dto.AkskDTO;
import com.dev.lib.aksk.domain.model.vo.AkskVO;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AkskService {

    AkskVO.CreateResult create(AkskDTO.Create cmd);

    void update(AkskDTO.Update cmd);

    void delete(String id);

    void enable(String id);

    void disable(String id);

    AkskVO.ResetSecretResult resetSecret(String id);

    ServerResponse<List<AkskVO.ListItem>> page(QueryRequest<AkskDTO.Query> request);

    AkskVO.Detail detail(String id);

    Optional<AkskCredential.Entity> findActiveByAccessKey(String accessKey);

    void touchLastUsed(String id, String ip, LocalDateTime time);
}
