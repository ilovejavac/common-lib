package com.dev.lib.jpa.entity.dsl;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {

    Map<SFunction<?, ?>, SerializedLambda> LAMBDA_CACHE = new ConcurrentHashMap<>();

    default SerializedLambda getSerializedLambda() {

        return LAMBDA_CACHE.computeIfAbsent(
                this, fn -> {
                    try {
                        Method method = fn.getClass().getDeclaredMethod("writeReplace");
                        method.setAccessible(true);
                        return (SerializedLambda) method.invoke(fn);
                    } catch (Exception e) {
                        throw new IllegalStateException("无法解析 Lambda", e);
                    }
                }
        );
    }

    // SFunction.java
    default String getFieldName() {
        String methodName = getSerializedLambda().getImplMethodName();

        // Java getter: getName -> name
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }

        // Kotlin 属性访问: 直接就是属性名（如 name, description）
        // 检查是否是有效的属性名（小写字母开头，不含特殊字符）
        if (Character.isLowerCase(methodName.charAt(0)) && !methodName.contains("$")) {
            return methodName;
        }

        System.out.println(methodName);

        throw new IllegalStateException("不是标准 getter: " + methodName);
    }

    @SuppressWarnings("unchecked")
    default Class<T> getEntityClass() {

        try {
            String className = getSerializedLambda().getImplClass().replace('/', '.');
            return (Class<T>) Class.forName(className);
        } catch (Exception e) {
            throw new IllegalStateException("无法解析实体类", e);
        }
    }

}
