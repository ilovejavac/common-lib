package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileObjectRepository;
import com.dev.lib.storage.domain.model.StorageType;
import com.dev.lib.storage.domain.service.StorageServiceNameProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractChainStorageTest {

    @Test
    void saveFileRecordShouldReturnSavedFile() {

        SysFileObjectRepository fileRepository = mock(SysFileObjectRepository.class);
        TestChainStorage storage = new TestChainStorage(
                storageProperties(),
                fileRepository,
                new StorageServiceNameProvider("test-service")
        );
        SysFile persistedFile = new SysFile();
        persistedFile.setBizId("file-1");

        when(fileRepository.findByBucketNameAndObjectKeyForUpdate(
                "test-service",
                "documents",
                "reports/summary.pdf"
        )).thenReturn(Optional.empty());
        when(fileRepository.save(any(SysFile.class))).thenReturn(persistedFile);

        SysFile result = storage.save("documents", "reports/summary.pdf", "/data/summary.pdf", 128L);

        assertThat(result).isSameAs(persistedFile);
    }

    private static AppStorageProperties storageProperties() {

        AppStorageProperties properties = new AppStorageProperties();
        properties.setType(StorageType.LOCAL);
        AppStorageProperties.Local local = new AppStorageProperties.Local();
        local.setPath("/tmp/storage");
        properties.setLocal(local);
        return properties;
    }

    private static final class TestChainStorage extends AbstractChainStorage {

        private TestChainStorage(
                AppStorageProperties fileProperties,
                SysFileObjectRepository fileRepository,
                StorageServiceNameProvider serviceNameProvider
        ) {
            super(fileProperties, fileRepository, serviceNameProvider);
        }

        private SysFile save(String bucketName, String objectKey, String storagePath, Long size) {
            return saveFileRecord(bucketName, objectKey, storagePath, size);
        }
    }
}
