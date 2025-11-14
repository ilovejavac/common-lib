package com.dev.lib.web.dict;

import com.dev.lib.web.dict.pojo.DictItemRepository;
import com.dev.lib.web.dict.pojo.DictTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;


}