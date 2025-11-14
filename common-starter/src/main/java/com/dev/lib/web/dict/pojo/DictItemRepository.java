package com.dev.lib.web.dict.pojo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DictItemRepository extends JpaRepository<DictItemEntity, Long> {

    Optional<DictItemEntity> findByItemCodeAndStatus(String itemCode, Integer status);

    List<DictItemEntity> findByItemCodeInAndStatus(Collection<String> itemCodes, Integer status);

    List<DictItemEntity> findByDictType_TypeCodeAndStatusOrderBySort(String typeCode, Integer status);
}