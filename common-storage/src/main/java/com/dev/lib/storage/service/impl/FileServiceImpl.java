package com.dev.lib.storage.service.impl;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileBizIdRepository;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final SysFileBizIdRepository fileRepository;

    private final StorageServiceNameProvider serviceNameProvider;

    @Override
    public Optional<SysFile> findByBizId(String bizId) {

        if (bizId == null || bizId.isBlank()) {
            return Optional.empty();
        }
        return fileRepository.findByBizIdAndServiceName(bizId, serviceNameProvider.currentServiceName());
    }

}
