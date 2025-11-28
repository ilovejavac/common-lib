package com.dev.lib.web.serialize;

import com.dev.lib.web.model.ServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * AOP 切面：在 Controller 返回后、序列化前，批量加载填充数据到缓存
 */
@Aspect
@Component
@Order(1)
@Slf4j
public class PopulateFieldAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();

        try {
            // 提取数据部分
            Object dataToScan = result;
            if (result instanceof ServerResponse<?> response) {
                dataToScan = response.getData();
            }

            // 提取所有 @PopulateField 字段，按 loader 分组
            Map<String, Set<Object>> loaderKeyMap = PopulateFieldExtractor.extract(dataToScan);


            // 批量加载每个 loader
            for (Map.Entry<String, Set<Object>> entry : loaderKeyMap.entrySet()) {
                String loaderName = entry.getKey();
                Set<Object> keys = entry.getValue();

                PopulateLoader<Object, Object> loader = PopulateLoaderRegistry.getLoader(loaderName);
                if (loader != null) {
                    PopulateContextHolder.preload(loaderName, keys, loader);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to preload populate fields", e);
        }

        return result;
    }
}