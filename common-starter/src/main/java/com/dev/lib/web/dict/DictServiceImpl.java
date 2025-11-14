package com.dev.lib.web.dict;

import com.dev.lib.web.dict.pojo.DictItemEntity;
import com.dev.lib.web.dict.pojo.DictItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictItemRepository dictItemRepository;

    @Override
    @Cacheable(value = "dict", key = "#code")
    public DictItem getItem(String code) {
        return dictItemRepository.findByItemCodeAndStatus(code, 1)
                .map(this::toItem)
                .orElse(null);
    }

    @Override
    public Map<String, DictItem> getItems(Collection<String> codes) {
        return dictItemRepository.findByItemCodeInAndStatus(codes, 1).stream()
                .collect(Collectors.toMap(
                        DictItemEntity::getItemCode,
                        this::toItem
                ));
    }

    private DictItem toItem(DictItemEntity entity) {
        return new DictItem(
                entity.getItemCode(),
                entity.getItemLabel(),
                entity.getCss()
        );
    }
}