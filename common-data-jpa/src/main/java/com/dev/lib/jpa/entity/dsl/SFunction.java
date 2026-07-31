package com.dev.lib.jpa.entity.dsl;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 可序列化的字段引用，用于把实体 getter 方法引用解析为属性名。
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {

    Map<SFunction<?, ?>, SerializedLambda> LAMBDA_CACHE = new ConcurrentHashMap<>();

    default SerializedLambda getSerializedLambda() {

        return LAMBDA_CACHE.computeIfAbsent(
                this,
                fn -> {
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
        if (Character.isLowerCase(methodName.charAt(0)) && !methodName.contains("$")) {
            return methodName;
        }

        throw new IllegalStateException("不是标准 getter: " + methodName);
    }

}
