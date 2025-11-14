package com.dev.lib.web.dict.pojo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends JpaRepository<DictType, Long> {
    Optional<DictType> findByTypeCode(String typeCode);
}