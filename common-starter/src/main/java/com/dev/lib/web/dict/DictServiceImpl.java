package com.dev.lib.web.dict;

import com.dev.lib.web.dict.data.DictItemEntityToDictItemMapper;
import com.dev.lib.web.dict.data.DictItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictItemEntityToDictItemMapper mapper;
    private final DictItemRepository itemRepository;

    @Override
    public DictItem getItem(String code) {
        return mapper.convert(itemRepository.getItem(code));
    }

    @Override
    public Map<String, DictItem> getItems(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }

        return itemRepository.listItem(codes).stream()
                .map(mapper::convert)
                .collect(Collectors.toMap(DictItem::getCode, item -> item));
    }
}
