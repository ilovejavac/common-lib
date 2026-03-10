package com.dev.lib.storage.service;

import com.dev.lib.storage.data.SysFile;

import java.util.Optional;

/**
 * 文件服务（对外）
 */
public interface FileService {

    /**
     * 通过 bizId 查询当前服务下的文件
     *
     * @param bizId 文件业务 ID
     * @return 文件记录
     */
    Optional<SysFile> findByBizId(String bizId);

}
