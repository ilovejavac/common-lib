package com.dev.lib.storage.domain.adapter;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.data.SysFileToStorageFileMapper;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageFileToSysFileMapper;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StorageFileAdapt implements StorageFileRepo {

    private final SysFileRepository fileRepository;

    private final SysFileToStorageFileMapper storageFileMapper;

    private final StorageFileToSysFileMapper sysFileMapper;

    private final StorageServiceNameProvider serviceNameProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(String bizId) {

        Optional<SysFile> loadedFile = fileRepository.findByBizIdAndServiceName(
                bizId,
                serviceNameProvider.currentServiceName()
        );
        loadedFile.ifPresent(fileRepository::delete);
    }

    @Override
    public StorageFile findByBizId(String value) {

        return storageFileMapper.convert(
                fileRepository.findByBizIdAndServiceName(value, serviceNameProvider.currentServiceName()).orElse(null)
        );
    }

    @Override
    public void saveFile(StorageFile storageFile) {
        SysFile sysFile = sysFileMapper.convert(storageFile);
        if (sysFile.getServiceName() == null || sysFile.getServiceName().isBlank()) {
            sysFile.setServiceName(serviceNameProvider.currentServiceName());
        }
        fileRepository.save(sysFile);
    }

    @Override
    public List<StorageFile> findByIds(Collection<String> ids) {

        return fileRepository.findAllByBizIdInAndServiceName(ids, serviceNameProvider.currentServiceName())
                .stream()
                .map(storageFileMapper::convert)
                .toList();
    }

    @Override
    public Collection<String> collectRemovePath(Collection<String> ids) {

        List<SysFile>   files = fileRepository.findAllByBizIdInAndServiceName(ids, serviceNameProvider.currentServiceName());
        HashSet<String> paths = new HashSet<>();
        for (SysFile file : files) {
            paths.add(file.getStoragePath());
            if (file.getOldStoragePaths() != null) {
                paths.addAll(file.getOldStoragePaths());
            }
        }

        return paths;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllByIds(Collection<String> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }
        fileRepository.deleteAllByBizIdInAndServiceName(ids, serviceNameProvider.currentServiceName());
    }

}
