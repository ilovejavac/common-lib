package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.aksk.domain.model.dto.AkskDTO;
import com.dev.lib.aksk.domain.model.vo.AkskVO;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AkskServiceTest {

    @Mock
    private AkskCredential.Mapper mapper;

    private final AkskKeyGenerator keyGenerator = new FixedKeyGenerator();

    private final RecordingConverter converter = new RecordingConverter();

    @Test
    void createShouldSaveActiveCredentialAndReturnPlainSecretOnce() {

        AkskService service = service();
        when(mapper.save(any(AkskCredential.Entity.class))).thenAnswer(invocation -> {
            AkskCredential.Entity entity = invocation.getArgument(0);
            entity.setBizId("cred-1");
            return entity;
        });

        AkskDTO.Create cmd = new AkskDTO.Create();
        cmd.setSubjectName("Datares");
        cmd.setSubjectCode("datares");
        cmd.setScopes(new LinkedHashSet<>(Set.of("datares:ads:query")));
        cmd.setProperties(new LinkedHashMap<>(Map.of("tenant", "blue")));

        AkskVO.CreateResult result = service.create(cmd);

        ArgumentCaptor<AkskCredential.Entity> captor = ArgumentCaptor.forClass(AkskCredential.Entity.class);
        verify(mapper).save(captor.capture());
        AkskCredential.Entity saved = captor.getValue();

        assertThat(converter.createToEntityCount).isEqualTo(1);
        assertThat(saved.getAccessKey()).isEqualTo("ak_fixed");
        assertThat(saved.getSecretKey()).isEqualTo("sk_fixed_1");
        assertThat(saved.getStatus()).isEqualTo(AkskStatus.active);
        assertThat(saved.getSubjectName()).isEqualTo("Datares");
        assertThat(saved.getSubjectCode()).isEqualTo("datares");
        assertThat(saved.getScopes()).containsExactly("datares:ads:query");
        assertThat(saved.getProperties()).containsEntry("tenant", "blue");

        assertThat(result.getId()).isEqualTo("cred-1");
        assertThat(result.getAccessKey()).isEqualTo("ak_fixed");
        assertThat(result.getSecretKey()).isEqualTo("sk_fixed_1");
    }

    @Test
    void pageAndDetailShouldNeverExposePlainSecret() {

        AkskService service = service();
        AkskCredential.Entity entity = credential("cred-1", AkskStatus.active);
        entity.setSecretKey("sk_sensitive");
        when(mapper.page(any(AkskCredential.Query.class))).thenReturn(new PageImpl<>(
                List.of(entity),
                PageRequest.of(0, 20),
                1
        ));
        when(mapper.findByBizId("cred-1")).thenReturn(Optional.of(entity));

        QueryRequest<AkskDTO.Query> request = new QueryRequest<>();
        request.setQuery(new AkskDTO.Query());
        ServerResponse<List<AkskVO.ListItem>> page = service.page(request);
        AkskVO.Detail detail = service.detail("cred-1");

        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().getFirst().getId()).isEqualTo("cred-1");
        assertThat(page.getData().getFirst().getAccessKey()).isEqualTo("ak_cred-1");
        assertThat(detail.getId()).isEqualTo("cred-1");
        assertThat(detail.getAccessKey()).isEqualTo("ak_cred-1");
        assertThat(converter.entityToListItemCount).isEqualTo(1);
        assertThat(converter.entityToDetailCount).isEqualTo(1);
        assertThat(fieldNames(AkskVO.ListItem.class)).doesNotContain("secretKey");
        assertThat(fieldNames(AkskVO.Detail.class)).doesNotContain("secretKey");
    }

    @Test
    void updateShouldApplyNonNullFieldsThroughAutoMapper() {

        AkskService service = service();
        AkskCredential.Entity entity = credential("cred-1", AkskStatus.active);
        entity.setSubjectName("Before");
        entity.setSubjectCode("keep-code");
        when(mapper.findByBizId("cred-1")).thenReturn(Optional.of(entity));

        AkskDTO.Update cmd = new AkskDTO.Update();
        cmd.setBizId("cred-1");
        cmd.setSubjectName("After");
        cmd.setSubjectCode(null);
        cmd.setScopes(new LinkedHashSet<>(Set.of("scope:new")));

        service.update(cmd);

        assertThat(converter.updateToEntityCount).isEqualTo(1);
        assertThat(entity.getSubjectName()).isEqualTo("After");
        assertThat(entity.getSubjectCode()).isEqualTo("keep-code");
        assertThat(entity.getScopes()).containsExactly("scope:new");
        verify(mapper).save(entity);
    }

    @Test
    void resetSecretShouldChangeStoredSecretAndReturnNewPlainSecretOnce() {

        AkskService service = service();
        AkskCredential.Entity entity = credential("cred-1", AkskStatus.active);
        entity.setSecretKey("sk_old");
        when(mapper.findByBizId("cred-1")).thenReturn(Optional.of(entity));
        when(mapper.save(any(AkskCredential.Entity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AkskVO.ResetSecretResult result = service.resetSecret("cred-1");

        assertThat(entity.getSecretKey()).isEqualTo("sk_fixed_1");
        assertThat(result.getId()).isEqualTo("cred-1");
        assertThat(result.getAccessKey()).isEqualTo("ak_cred-1");
        assertThat(result.getSecretKey()).isEqualTo("sk_fixed_1");
        verify(mapper).save(entity);
    }

    @Test
    void enableShouldDelegateToBulkStatusUpdateAndOnlyReadWhenNoRowsChanged() {

        AkskService service = service();
        AkskCredential.Entity active = credential("cred-2", AkskStatus.active);
        when(mapper.markActive("cred-1")).thenReturn(true);
        when(mapper.markActive("cred-2")).thenReturn(false);
        when(mapper.findByBizId("cred-2")).thenReturn(Optional.of(active));

        service.enable("cred-1");
        service.enable("cred-2");

        verify(mapper).markActive("cred-1");
        verify(mapper).markActive("cred-2");
        verify(mapper, never()).findByBizId("cred-1");
        verify(mapper, never()).save(any(AkskCredential.Entity.class));
    }

    @Test
    void disableShouldDelegateToBulkStatusUpdateAndOnlyReadWhenNoRowsChanged() {

        AkskService service = service();
        AkskCredential.Entity disabled = credential("cred-2", AkskStatus.disable);
        when(mapper.markDisable("cred-1")).thenReturn(true);
        when(mapper.markDisable("cred-2")).thenReturn(false);
        when(mapper.findByBizId("cred-2")).thenReturn(Optional.of(disabled));

        service.disable("cred-1");
        service.disable("cred-2");

        verify(mapper).markDisable("cred-1");
        verify(mapper).markDisable("cred-2");
        verify(mapper, never()).findByBizId("cred-1");
        verify(mapper, never()).save(any(AkskCredential.Entity.class));
    }

    @Test
    void touchLastUsedShouldDelegateToBulkRepositoryHelper() {

        AkskService service = service();
        LocalDateTime usedAt = LocalDateTime.of(2026, 5, 8, 12, 20);

        service.touchLastUsed("cred-1", "192.0.2.20", usedAt);

        verify(mapper).touchLastUsed("cred-1", "192.0.2.20", usedAt);
        verify(mapper, never()).findByBizId("cred-1");
        verify(mapper, never()).save(any(AkskCredential.Entity.class));
    }

    @Test
    void scopesAndPropertiesShouldBePersistedAsCollections() {

        AkskService service = service();
        when(mapper.save(any(AkskCredential.Entity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AkskDTO.Create cmd = new AkskDTO.Create();
        cmd.setSubjectName("Partner");
        cmd.setScopes(new LinkedHashSet<>(Set.of("scope:a", "scope:b")));
        cmd.setProperties(new LinkedHashMap<>(Map.of("env", "prod", "owner", "ops")));

        service.create(cmd);

        ArgumentCaptor<AkskCredential.Entity> captor = ArgumentCaptor.forClass(AkskCredential.Entity.class);
        verify(mapper).save(captor.capture());
        assertThat(captor.getValue().getScopes()).containsExactlyInAnyOrder("scope:a", "scope:b");
        assertThat(captor.getValue().getProperties())
                .containsEntry("env", "prod")
                .containsEntry("owner", "ops");
    }

    private AkskService service() {

        return new AkskServiceImpl(mapper, keyGenerator, converter);
    }

    private AkskCredential.Entity credential(String id, AkskStatus status) {

        AkskCredential.Entity entity = new AkskCredential.Entity();
        entity.setBizId(id);
        entity.setAccessKey("ak_" + id);
        entity.setSecretKey("sk_" + id);
        entity.setSubjectName("Subject " + id);
        entity.setSubjectCode("subject-" + id);
        entity.setScopes(new LinkedHashSet<>(Set.of("scope:a")));
        entity.setProperties(new LinkedHashMap<>(Map.of("owner", id)));
        entity.setStatus(status);
        entity.setExpireAt(LocalDateTime.now().plusDays(1));
        return entity;
    }

    private List<String> fieldNames(Class<?> type) {

        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .toList();
    }

    private static class FixedKeyGenerator extends AkskKeyGenerator {

        @Override
        public String generateAccessKey() {

            return "ak_fixed";
        }

        @Override
        public String generateSecretKey() {

            return "sk_fixed_1";
        }
    }

    private static class RecordingConverter extends Converter {

        private int createToEntityCount;

        private int updateToEntityCount;

        private int entityToListItemCount;

        private int entityToDetailCount;

        @Override
        @SuppressWarnings("unchecked")
        public <S, T> T convert(S source, Class<T> target) {

            if (source instanceof AkskDTO.Create cmd && target == AkskCredential.Entity.class) {
                createToEntityCount++;
                AkskCredential.Entity entity = new AkskCredential.Entity();
                entity.setSubjectName(cmd.getSubjectName());
                entity.setSubjectCode(cmd.getSubjectCode());
                entity.setContactName(cmd.getContactName());
                entity.setContactPhone(cmd.getContactPhone());
                entity.setDescription(cmd.getDescription());
                entity.setScopes(cmd.getScopes() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(cmd.getScopes()));
                entity.setProperties(cmd.getProperties() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cmd.getProperties()));
                entity.setExpireAt(cmd.getExpireAt());
                return (T) entity;
            }
            if (source instanceof AkskCredential.Entity entity && target == AkskVO.ListItem.class) {
                entityToListItemCount++;
                AkskVO.ListItem item = new AkskVO.ListItem();
                item.setId("wrong-id");
                item.setAccessKey(entity.getAccessKey());
                item.setSubjectName(entity.getSubjectName());
                item.setSubjectCode(entity.getSubjectCode());
                item.setScopes(entity.getScopes() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(entity.getScopes()));
                item.setStatus(entity.getStatus());
                item.setExpireAt(entity.getExpireAt());
                item.setLastUsedAt(entity.getLastUsedAt());
                item.setLastUsedIp(entity.getLastUsedIp());
                return (T) item;
            }
            if (source instanceof AkskCredential.Entity entity && target == AkskVO.Detail.class) {
                entityToDetailCount++;
                AkskVO.Detail detail = new AkskVO.Detail();
                detail.setId("wrong-id");
                detail.setAccessKey(entity.getAccessKey());
                detail.setSubjectName(entity.getSubjectName());
                detail.setSubjectCode(entity.getSubjectCode());
                detail.setContactName(entity.getContactName());
                detail.setContactPhone(entity.getContactPhone());
                detail.setDescription(entity.getDescription());
                detail.setScopes(entity.getScopes() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(entity.getScopes()));
                detail.setProperties(entity.getProperties() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(entity.getProperties()));
                detail.setStatus(entity.getStatus());
                detail.setExpireAt(entity.getExpireAt());
                detail.setLastUsedAt(entity.getLastUsedAt());
                detail.setLastUsedIp(entity.getLastUsedIp());
                return (T) detail;
            }
            throw new AssertionError("Unexpected converter call: " + source + " -> " + target);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S, T> T convert(S source, T target) {

            if (source instanceof AkskDTO.Update cmd && target instanceof AkskCredential.Entity entity) {
                updateToEntityCount++;
                if (cmd.getSubjectName() != null) {
                    entity.setSubjectName(cmd.getSubjectName());
                }
                if (cmd.getSubjectCode() != null) {
                    entity.setSubjectCode(cmd.getSubjectCode());
                }
                if (cmd.getContactName() != null) {
                    entity.setContactName(cmd.getContactName());
                }
                if (cmd.getContactPhone() != null) {
                    entity.setContactPhone(cmd.getContactPhone());
                }
                if (cmd.getDescription() != null) {
                    entity.setDescription(cmd.getDescription());
                }
                if (cmd.getScopes() != null) {
                    entity.setScopes(new LinkedHashSet<>(cmd.getScopes()));
                }
                if (cmd.getProperties() != null) {
                    entity.setProperties(new LinkedHashMap<>(cmd.getProperties()));
                }
                if (cmd.getExpireAt() != null) {
                    entity.setExpireAt(cmd.getExpireAt());
                }
                return (T) entity;
            }
            throw new AssertionError("Unexpected converter update call: " + source + " -> " + target);
        }
    }
}
