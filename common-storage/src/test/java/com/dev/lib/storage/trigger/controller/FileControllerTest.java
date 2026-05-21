package com.dev.lib.storage.trigger.controller;

import com.dev.lib.storage.domain.adapter.StorageFileRepo;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.service.chain.ChainStorageService;
import com.dev.lib.web.model.ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerTest {

    @Test
    void downloadShouldUseRequestedNameAndStorageCoordinates() throws IOException {

        StorageFile storageFile = storageFile("biz-1", "bucket-a", "path/report.txt", "original.txt");
        RecordingChainStorageService storageService = new RecordingChainStorageService();
        FileController controller = new FileController(new FixedStorageFileRepo(storageFile), storageService);

        ResponseEntity<InputStreamResource> response = controller.download("biz-1", "renamed.txt");

        assertThat(storageService.lastDownloadBucket).isEqualTo("bucket-a");
        assertThat(storageService.lastDownloadObjectKey).isEqualTo("path/report.txt");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("renamed.txt");
    }

    @Test
    void downloadShouldFallbackToOriginalNameWhenRequestNameIsBlank() throws IOException {

        StorageFile storageFile = storageFile("biz-1", "bucket-a", "path/report.txt", "original.txt");
        RecordingChainStorageService storageService = new RecordingChainStorageService();
        FileController controller = new FileController(new FixedStorageFileRepo(storageFile), storageService);

        ResponseEntity<InputStreamResource> response = controller.download("biz-1", " ");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("original.txt");
    }

    @Test
    void getPresignedUrlShouldUseStorageCoordinates() {

        StorageFile storageFile = storageFile("biz-1", "bucket-a", "path/report.txt", "original.txt");
        RecordingChainStorageService storageService = new RecordingChainStorageService();
        storageService.presignedUrl = "https://example.com/report";
        FileController controller = new FileController(new FixedStorageFileRepo(storageFile), storageService);

        ResponseEntity<String> response = controller.getPresignedUrl("biz-1");

        assertThat(storageService.lastPresignedBucket).isEqualTo("bucket-a");
        assertThat(storageService.lastPresignedObjectKey).isEqualTo("path/report.txt");
        assertThat(response.getBody()).isEqualTo("https://example.com/report");
    }

    @Test
    void getPresignedUrlsShouldSkipFilesMissingStorageCoordinates() {

        StorageFile valid = storageFile("biz-1", "bucket-a", "path/report.txt", "original.txt");
        StorageFile missingKey = storageFile("biz-2", "bucket-a", null, "broken.txt");
        RecordingChainStorageService storageService = new RecordingChainStorageService();
        storageService.presignedUrl = "https://example.com/report";
        FileController controller = new FileController(new MapStorageFileRepo(Map.of(
                "biz-1", valid,
                "biz-2", missingKey
        )), storageService);

        ServerResponse<Map<String, String>> response = controller.getPresignedUrls(List.of("biz-1", "biz-2"));

        assertThat(response.getData()).containsOnlyKeys("biz-1");
    }

    private StorageFile storageFile(String bizId, String bucketName, String objectKey, String originalName) {

        StorageFile storageFile = new StorageFile();
        storageFile.setBizId(bizId);
        storageFile.setBucketName(bucketName);
        storageFile.setObjectKey(objectKey);
        storageFile.setOriginalName(originalName);
        return storageFile;
    }

    private static class FixedStorageFileRepo implements StorageFileRepo {

        private final StorageFile storageFile;

        private FixedStorageFileRepo(StorageFile storageFile) {

            this.storageFile = storageFile;
        }

        @Override
        public StorageFile findByBizId(String value) {

            return storageFile;
        }

        @Override
        public void remove(String bizId) {
        }

        @Override
        public void saveFile(StorageFile storageFile) {
        }

        @Override
        public List<StorageFile> findByIds(Collection<String> ids) {

            return List.of(storageFile);
        }

        @Override
        public Collection<String> collectRemovePath(Collection<String> ids) {

            return List.of();
        }

        @Override
        public void removeAllByIds(Collection<String> ids) {
        }
    }

    private static class MapStorageFileRepo implements StorageFileRepo {

        private final Map<String, StorageFile> storageFiles;

        private MapStorageFileRepo(Map<String, StorageFile> storageFiles) {

            this.storageFiles = storageFiles;
        }

        @Override
        public StorageFile findByBizId(String value) {

            return storageFiles.get(value);
        }

        @Override
        public void remove(String bizId) {
        }

        @Override
        public void saveFile(StorageFile storageFile) {
        }

        @Override
        public List<StorageFile> findByIds(Collection<String> ids) {

            return ids.stream()
                    .map(storageFiles::get)
                    .filter(file -> file != null)
                    .toList();
        }

        @Override
        public Collection<String> collectRemovePath(Collection<String> ids) {

            return List.of();
        }

        @Override
        public void removeAllByIds(Collection<String> ids) {
        }
    }

    private static class RecordingChainStorageService implements ChainStorageService {

        private String lastDownloadBucket;
        private String lastDownloadObjectKey;
        private String lastPresignedBucket;
        private String lastPresignedObjectKey;
        private String presignedUrl = "https://example.com/default";

        @Override
        public String upload(String bucketName, String objectKey, org.springframework.web.multipart.MultipartFile file) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String upload(String bucketName, String objectKey, InputStream inputStream) {

            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream download(String bucketName, String objectKey) {

            lastDownloadBucket = bucketName;
            lastDownloadObjectKey = objectKey;
            return new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void delete(String bucketName, String objectKey) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String getPresignedUrl(String bucketName, String objectKey, int expireSeconds) {

            lastPresignedBucket = bucketName;
            lastPresignedObjectKey = objectKey;
            return presignedUrl;
        }

        @Override
        public String copy(String bucketName, String sourceKey, String targetKey) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String append(String bucketName, String objectKey, String content) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String appendBytes(String bucketName, String objectKey, byte[] bytes) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String write(String bucketName, String objectKey, String content) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String writeBytes(String bucketName, String objectKey, byte[] bytes) {

            throw new UnsupportedOperationException();
        }

        @Override
        public String replaceLines(String bucketName, String objectKey, com.dev.lib.storage.Storage.LineTransformer transformer) {

            throw new UnsupportedOperationException();
        }

    }
}
