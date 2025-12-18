package com.dev.lib.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer, InitializingBean {

    private final NullToEmptyFilter nullToEmptyFilter;

    private final PopulateFieldAfterFilter populateFieldAfterFilter;

    private static final Set<String> EXCLUDE_FIELDS = Set.of(
            "id",
            "reversion",
            "deleted"
    );

    @Override
    public void afterPropertiesSet() {

        JSON.register(
                Instant.class,
                new FastJson2Support.InstantWriter()
        );
        JSON.registerIfAbsent(
                Instant.class,
                new FastJson2Support.InstantReader()
        );
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig               config    = new FastJsonConfig();

        config.setDateFormat(FastJson2Support.DATE_FORMAT);
        config.setCharset(StandardCharsets.UTF_8);
        config.setWriterFeatures(FastJson2Support.WRITER_FEATURES);
        config.setReaderFeatures(FastJson2Support.READER_FEATURES);

        // 过滤器
        config.setWriterFilters(
                nullToEmptyFilter,
                // 排除字段
                (PropertyFilter) (obj, name, value) -> !EXCLUDE_FIELDS.contains(name),
                // BigDecimal 6位小数 + Instant 时区转换 + Long 精度保护
                FastJson2Support.VALUE_FILTER,
                // PopulateField 填充
                populateFieldAfterFilter
        );

        converter.setFastJsonConfig(config);
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType(
                        "application",
                        "*+json"
                )
        ));

        converters.addFirst(converter);
    }

    //    @Override
//    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
//
//        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
//        FastJsonConfig               config    = new FastJsonConfig();
//
//        config.setDateFormat(FastJson2Support.DATE_FORMAT);
//        config.setCharset(StandardCharsets.UTF_8);
//        config.setWriterFeatures(FastJson2Support.WRITER_FEATURES);
//        config.setReaderFeatures(FastJson2Support.READER_FEATURES);
//
//        // 过滤器
//        config.setWriterFilters(
//                nullToEmptyFilter,
//                // 排除字段
//                (PropertyFilter) (obj, name, value) -> !EXCLUDE_FIELDS.contains(name),
//                // BigDecimal 6位小数 + Instant 时区转换 + Long 精度保护
//                FastJson2Support.VALUE_FILTER,
//                // PopulateField 填充
//                populateFieldAfterFilter
//        );
//
//        converter.setFastJsonConfig(config);
//        converter.setSupportedMediaTypes(List.of(
//                MediaType.APPLICATION_JSON,
//                new MediaType(
//                        "application",
//                        "*+json"
//                )
//        ));
//
//        builder.withJsonConverter(converter);
//    }

}