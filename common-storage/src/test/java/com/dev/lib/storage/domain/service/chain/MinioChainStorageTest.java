package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileObjectRepository;
import com.dev.lib.storage.domain.model.StorageType;
import com.dev.lib.storage.domain.service.StorageServiceNameProvider;
import io.minio.*;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinioChainStorageTest {

    @Test
    void uploadShouldReturnSavedFile() throws Exception {

        MinioClient minioClient = mock(MinioClient.class);
        SysFileObjectRepository fileRepository = mock(SysFileObjectRepository.class);
        MinioChainStorage storage = new MinioChainStorage(
                storageProperties(),
                fileRepository,
                new StorageServiceNameProvider("test-service")
        );
        ReflectionTestUtils.setField(storage, "minioClient", minioClient);
        SysFile persistedFile = new SysFile();
        persistedFile.setBizId("file-1");

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(fileRepository.findByBucketNameAndObjectKeyForUpdate("test-service", "boms", "hello.txt"))
                .thenReturn(Optional.empty());
        when(fileRepository.save(any(SysFile.class))).thenReturn(persistedFile);

        SysFile result = storage.upload("boms", "hello.txt", new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(result).isSameAs(persistedFile);
    }

    @Test
    void appendShouldRewriteSmallExistingObjectInsteadOfComposing() throws Exception {

        MinioClient minioClient = mock(MinioClient.class);
        SysFileObjectRepository fileRepository = mock(SysFileObjectRepository.class);
        MinioChainStorage storage = new MinioChainStorage(
                storageProperties(),
                fileRepository,
                new StorageServiceNameProvider("test-service")
        );
        ReflectionTestUtils.setField(storage, "minioClient", minioClient);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        StatObjectResponse statObjectResponse = mock(StatObjectResponse.class);
        when(statObjectResponse.size()).thenReturn(10L);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(new GetObjectResponse(
                Headers.of(),
                "boms",
                null,
                "hello.txt",
                new ByteArrayInputStream("hello word".getBytes(StandardCharsets.UTF_8))
        ));
        when(fileRepository.findByBucketNameAndObjectKeyForUpdate("test-service", "boms", "hello.txt"))
                .thenReturn(Optional.empty());
        when(fileRepository.save(any(SysFile.class))).thenAnswer(invocation -> {
            SysFile file = invocation.getArgument(0);
            file.setBizId("file-1");
            return file;
        });

        String bizId = storage.appendBytes("boms", "hello.txt", "!".getBytes(StandardCharsets.UTF_8));

        assertThat(bizId).isEqualTo("file-1");
        verify(minioClient, never()).composeObject(any(ComposeObjectArgs.class));

        ArgumentCaptor<PutObjectArgs> putObjectArgs = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(putObjectArgs.capture());
        PutObjectArgs writtenObject = putObjectArgs.getValue();
        assertThat(writtenObject.bucket()).isEqualTo("boms");
        assertThat(writtenObject.object()).isEqualTo("hello.txt");
        assertThat(new String(writtenObject.stream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("hello word!");

        ArgumentCaptor<SysFile> savedFile = ArgumentCaptor.forClass(SysFile.class);
        verify(fileRepository).save(savedFile.capture());
        assertThat(savedFile.getValue().getSize()).isEqualTo(11L);
    }

    private AppStorageProperties storageProperties() {

        AppStorageProperties properties = new AppStorageProperties();
        properties.setType(StorageType.MINIO);

        AppStorageProperties.Minio minio = new AppStorageProperties.Minio();
        minio.setEndpoint("http://127.0.0.1:9000");
        minio.setAccessKey("test-access");
        minio.setSecretKey("test-secret");
        minio.setBucket("boms");
        properties.setMinio(minio);
        return properties;
    }
}
