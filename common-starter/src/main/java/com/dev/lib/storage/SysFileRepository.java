package com.dev.lib.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SysFileRepository extends JpaRepository<SysFile, Long> {
    SysFile findByMd5(String md5);
}
