package com.dev.lib.web.dict;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DictTypeMapper {
    DictTypeMapper INS = Mappers.getMapper(DictTypeMapper.class);

}
