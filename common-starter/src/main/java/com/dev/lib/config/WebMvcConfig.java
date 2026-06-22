package com.dev.lib.config;

import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebMvcConfig {

    @Bean
    public FastJsonHttpMessageConverter commonFastJsonHttpMessageConverter() {

        FastJsonSupport.configure();
        FastJsonConfig config = new FastJsonConfig();
        config.setCharset(StandardCharsets.UTF_8);
        config.setDateFormat(FastJsonSupport.DATE_FORMAT);
        config.setReaderFeatures(FastJsonSupport.READER_FEATURES);
        config.setWriterFeatures(FastJsonSupport.WRITER_FEATURES);
        config.setWriterFilters(FastJsonSupport.WRITER_FILTERS);
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setFastJsonConfig(config);
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        return converter;
    }

    @Bean
    public ServerHttpMessageConvertersCustomizer commonServerHttpMessageConvertersCustomizer(
            FastJsonHttpMessageConverter commonFastJsonHttpMessageConverter
    ) {

        return builder -> builder.withJsonConverter(commonFastJsonHttpMessageConverter);
    }

    @Bean
    public ClientHttpMessageConvertersCustomizer commonClientHttpMessageConvertersCustomizer(
            FastJsonHttpMessageConverter commonFastJsonHttpMessageConverter
    ) {

        return builder -> builder.withJsonConverter(commonFastJsonHttpMessageConverter);
    }
}
