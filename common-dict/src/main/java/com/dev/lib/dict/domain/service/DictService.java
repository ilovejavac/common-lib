package com.dev.lib.dict.domain.service;

import com.dev.lib.dict.serialize.DictItem;

import java.util.Collection;
import java.util.Map;

public interface DictService {

    DictItem getItem(String code);

    Map<String, DictItem> getItems(Collection<String> codes);
}