package com.dev.lib.aksk.trigger;

import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.model.dto.AkskDTO;
import com.dev.lib.aksk.domain.model.vo.AkskVO;
import com.dev.lib.aksk.domain.service.AkskService;
import com.dev.lib.web.model.QueryRequest;
import com.dev.lib.web.model.ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AkskAdminControllerTest {

    @Test
    void controllerShouldExposeAdminRoutes() throws NoSuchMethodException {

        assertThat(AkskAdminController.class).hasAnnotation(RestController.class);
        assertThat(AkskAdminController.class.getAnnotation(RequestMapping.class)).isNull();

        assertPostRoute("create", "/admin/aksk/create_aksk", AkskDTO.Create.class);
        assertPostRoute("update", "/admin/aksk/update_aksk", AkskDTO.Update.class);
        assertPostRoute("delete", "/admin/aksk/delete_aksk/{id}", String.class);
        assertPostRoute("enable", "/admin/aksk/enable_aksk/{id}", String.class);
        assertPostRoute("disable", "/admin/aksk/disable_aksk/{id}", String.class);
        assertPostRoute("resetSecret", "/admin/aksk/reset_secret_aksk/{id}", String.class);
        assertPostRoute("queryList", "/admin/aksk/query_list_aksk", QueryRequest.class);

        Method detail = AkskAdminController.class.getMethod("detail", String.class);
        assertThat(detail.getAnnotation(GetMapping.class).value()).containsExactly("/admin/aksk/detail_aksk/{id}");
    }

    @Test
    void createShouldReturnAccessKeyAndOneTimePlainSecret() {

        RecordingAkskService service = new RecordingAkskService();
        AkskAdminController controller = new AkskAdminController(service);

        AkskDTO.Create cmd = new AkskDTO.Create();
        cmd.setSubjectName("Datares");

        ServerResponse<AkskVO.CreateResult> response = controller.create(cmd);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getAccessKey()).isEqualTo("ak_created");
        assertThat(response.getData().getSecretKey()).isEqualTo("sk_created_once");
        assertThat(service.createdCommand).isSameAs(cmd);
    }

    @Test
    void queryListAndDetailShouldNeverReturnPlainSecret() {

        RecordingAkskService service = new RecordingAkskService();
        AkskAdminController controller = new AkskAdminController(service);
        QueryRequest<AkskDTO.Query> request = new QueryRequest<>();
        request.setQuery(new AkskDTO.Query());

        ServerResponse<List<AkskVO.ListItem>> listResponse = controller.queryList(request);
        ServerResponse<AkskVO.Detail> detailResponse = controller.detail("cred-1");

        assertThat(listResponse).isSameAs(service.pageResponse);
        assertThat(listResponse.getData()).hasSize(1);
        assertThat(detailResponse.getData().getAccessKey()).isEqualTo("ak_detail");
        assertThat(fieldNames(AkskVO.ListItem.class)).doesNotContain("secretKey");
        assertThat(fieldNames(AkskVO.Detail.class)).doesNotContain("secretKey");
        assertThat(service.queryRequest).isSameAs(request);
        assertThat(service.detailId).isEqualTo("cred-1");
    }

    @Test
    void resetSecretShouldReturnOneTimePlainSecret() {

        RecordingAkskService service = new RecordingAkskService();
        AkskAdminController controller = new AkskAdminController(service);

        ServerResponse<AkskVO.ResetSecretResult> response = controller.resetSecret("cred-1");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo("cred-1");
        assertThat(response.getData().getAccessKey()).isEqualTo("ak_reset");
        assertThat(response.getData().getSecretKey()).isEqualTo("sk_reset_once");
        assertThat(service.resetSecretId).isEqualTo("cred-1");
    }

    @Test
    void mutatingOperationsShouldDelegateToServiceAndReturnOk() {

        RecordingAkskService service = new RecordingAkskService();
        AkskAdminController controller = new AkskAdminController(service);
        AkskDTO.Update update = new AkskDTO.Update();
        update.setBizId("cred-1");

        assertThat(controller.update(update).getCode()).isEqualTo(200);
        assertThat(controller.delete("cred-2").getCode()).isEqualTo(200);
        assertThat(controller.enable("cred-3").getCode()).isEqualTo(200);
        assertThat(controller.disable("cred-4").getCode()).isEqualTo(200);

        assertThat(service.updatedCommand).isSameAs(update);
        assertThat(service.deletedId).isEqualTo("cred-2");
        assertThat(service.enabledId).isEqualTo("cred-3");
        assertThat(service.disabledId).isEqualTo("cred-4");
    }

    private void assertPostRoute(String methodName, String expectedRoute, Class<?>... parameterTypes)
            throws NoSuchMethodException {

        Method method = AkskAdminController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(expectedRoute);
    }

    private List<String> fieldNames(Class<?> type) {

        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .toList();
    }

    private static class RecordingAkskService implements AkskService {

        private final ServerResponse<List<AkskVO.ListItem>> pageResponse;

        private AkskDTO.Create createdCommand;

        private AkskDTO.Update updatedCommand;

        private String deletedId;

        private String enabledId;

        private String disabledId;

        private String resetSecretId;

        private QueryRequest<AkskDTO.Query> queryRequest;

        private String detailId;

        private RecordingAkskService() {

            AkskVO.ListItem item = new AkskVO.ListItem();
            item.setId("cred-1");
            item.setAccessKey("ak_list");
            item.setSubjectName("List Subject");
            this.pageResponse = ServerResponse.success(List.of(item));
        }

        @Override
        public AkskVO.CreateResult create(AkskDTO.Create cmd) {

            this.createdCommand = cmd;
            AkskVO.CreateResult result = new AkskVO.CreateResult();
            result.setId("cred-created");
            result.setAccessKey("ak_created");
            result.setSecretKey("sk_created_once");
            return result;
        }

        @Override
        public void update(AkskDTO.Update cmd) {

            this.updatedCommand = cmd;
        }

        @Override
        public void delete(String id) {

            this.deletedId = id;
        }

        @Override
        public void enable(String id) {

            this.enabledId = id;
        }

        @Override
        public void disable(String id) {

            this.disabledId = id;
        }

        @Override
        public AkskVO.ResetSecretResult resetSecret(String id) {

            this.resetSecretId = id;
            AkskVO.ResetSecretResult result = new AkskVO.ResetSecretResult();
            result.setId(id);
            result.setAccessKey("ak_reset");
            result.setSecretKey("sk_reset_once");
            return result;
        }

        @Override
        public ServerResponse<List<AkskVO.ListItem>> page(QueryRequest<AkskDTO.Query> request) {

            this.queryRequest = request;
            return pageResponse;
        }

        @Override
        public AkskVO.Detail detail(String id) {

            this.detailId = id;
            AkskVO.Detail detail = new AkskVO.Detail();
            detail.setId(id);
            detail.setAccessKey("ak_detail");
            detail.setSubjectName("Detail Subject");
            return detail;
        }

        @Override
        public Optional<AkskCredential.Entity> findActiveByAccessKey(String accessKey) {

            return Optional.empty();
        }

        @Override
        public void touchLastUsed(String id, String ip, LocalDateTime time) {

        }
    }
}
