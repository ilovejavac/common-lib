package com.dev.lib.dict.domain.service;

import com.dev.lib.dict.domain.adapter.DictAdapt;
import com.dev.lib.dict.serialize.DictItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictAdapt adapt;

    @Override
    public DictItem getItem(String code) {

        return adapt.getItem(code);
    }

    @Override
    public Map<String, DictItem> getItems(Collection<String> codes) {

        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }

        return adapt.listItem(codes).stream().collect(Collectors.toMap(
                DictItem::getItemCode,
                item -> item
        ));
    }

}
