package com.dev.lib.storage.domain.adapter;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.data.SysFileToStorageFileMapper;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageFileToSysFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StorageFileAdapt implements StorageFileRepo {

    private final SysFileRepository fileRepository;

    private final SysFileToStorageFileMapper storageFileMapper;

    private final StorageFileToSysFileMapper sysFileMapper;

    @Override
    public Optional<StorageFile> findByMd5(String md5) {

        return fileRepository.findByMd5(md5).map(storageFileMapper::convert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(String bizId) {

        Optional<SysFile> loadedFile = fileRepository.findByBizId(bizId);
        loadedFile.ifPresent(fileRepository::delete);
    }

    @Override
    public StorageFile findByBizId(String value) {

        return storageFileMapper.convert(fileRepository.findByBizId(value).orElse(null));
    }

    @Override
    public void saveFile(StorageFile storageFile) {
        // 保存记录
        fileRepository.save(sysFileMapper.convert(storageFile));
    }

    @Override
    public List<StorageFile> findByIds(Collection<String> ids) {

        return fileRepository.findAllByBizIdIn(ids).stream().map(storageFileMapper::convert).toList();
    }

}
