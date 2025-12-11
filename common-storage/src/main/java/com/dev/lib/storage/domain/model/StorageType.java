package com.dev.lib.storage.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StorageType {
    LOCAL("本地"),
    OSS("oss"),
    MINIO("minio");

    private final String msg;
}
