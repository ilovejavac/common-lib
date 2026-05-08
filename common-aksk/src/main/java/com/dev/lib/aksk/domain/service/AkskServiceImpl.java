package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.aksk.domain.model.dto.AkskDTO;
import com.dev.lib.aksk.domain.model.vo.AkskVO;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import com.dev.lib.web.model.StandardErrorCodes;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AkskServiceImpl implements AkskService {

    private final AkskCredential.Mapper mapper;

    private final AkskKeyGenerator keyGenerator;

    private final Converter converter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AkskVO.CreateResult create(AkskDTO.Create cmd) {

        if (cmd == null) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 创建参数不能为空");
        }

        String accessKey = keyGenerator.generateAccessKey();
        String secretKey = keyGenerator.generateSecretKey();

        AkskCredential.Entity entity = converter.convert(cmd, AkskCredential.Entity.class);
        entity.setAccessKey(accessKey);
        entity.setSecretKey(secretKey);
        entity.setStatus(AkskStatus.active);

        AkskCredential.Entity saved = mapper.save(entity);
        return createResult(saved, secretKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AkskDTO.Update cmd) {

        if (cmd == null || !StringUtils.hasText(cmd.getBizId())) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 凭证 ID 不能为空");
        }

        AkskCredential.Entity entity = requireCredential(cmd.getBizId());
        converter.convert(cmd, entity);
        mapper.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {

        mapper.delete(requireCredential(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(String id) {

        requireCredentialId(id);
        if (!mapper.markActive(id)) {
            requireCredentialExists(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(String id) {

        requireCredentialId(id);
        if (!mapper.markDisable(id)) {
            requireCredentialExists(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AkskVO.ResetSecretResult resetSecret(String id) {

        AkskCredential.Entity entity = requireCredential(id);
        String secretKey = keyGenerator.generateSecretKey();
        entity.setSecretKey(secretKey);
        AkskCredential.Entity saved = mapper.save(entity);
        return resetSecretResult(saved, secretKey);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerResponse<List<AkskVO.ListItem>> page(QueryRequest<AkskDTO.Query> request) {

        Page<AkskVO.ListItem> page = mapper.page(new AkskCredential.Query().external(request))
                .map(this::listItem);
        return ServerResponse.success(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AkskVO.Detail detail(String id) {

        return detail(requireCredential(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AkskCredential.Entity> findActiveByAccessKey(String accessKey) {

        if (!StringUtils.hasText(accessKey)) {
            return Optional.empty();
        }
        return mapper.loadByAccessKey(accessKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchLastUsed(String id, String ip, LocalDateTime time) {

        mapper.touchLastUsed(id, ip, time);
    }

    private AkskCredential.Entity requireCredential(String id) {

        requireCredentialId(id);
        return mapper.findByBizId(id)
                .orElseThrow(() -> new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 凭证不存在"));
    }

    private void requireCredentialId(String id) {

        if (!StringUtils.hasText(id)) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 凭证 ID 不能为空");
        }
    }

    private void requireCredentialExists(String id) {

        if (mapper.findByBizId(id).isEmpty()) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 凭证不存在");
        }
    }

    private AkskVO.CreateResult createResult(AkskCredential.Entity entity, String plainSecretKey) {

        AkskVO.CreateResult result = new AkskVO.CreateResult();
        result.setId(entity.getBizId());
        result.setAccessKey(entity.getAccessKey());
        result.setSecretKey(plainSecretKey);
        return result;
    }

    private AkskVO.ResetSecretResult resetSecretResult(AkskCredential.Entity entity, String plainSecretKey) {

        AkskVO.ResetSecretResult result = new AkskVO.ResetSecretResult();
        result.setId(entity.getBizId());
        result.setAccessKey(entity.getAccessKey());
        result.setSecretKey(plainSecretKey);
        return result;
    }

    private AkskVO.ListItem listItem(AkskCredential.Entity entity) {

        AkskVO.ListItem item = converter.convert(entity, AkskVO.ListItem.class);
        item.setId(entity.getBizId());
        return item;
    }

    private AkskVO.Detail detail(AkskCredential.Entity entity) {

        AkskVO.Detail detail = converter.convert(entity, AkskVO.Detail.class);
        detail.setId(entity.getBizId());
        return detail;
    }
}
