package com.dev.lib.web.dict;

import java.util.Collection;
import java.util.Map;

public interface DictService {

    DictItem getItem(String code);

    Map<String, DictItem> getItems(Collection<String> codes);
}