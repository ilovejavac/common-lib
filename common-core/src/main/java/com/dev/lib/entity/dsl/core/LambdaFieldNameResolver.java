package com.dev.lib.entity.dsl.core;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LambdaFieldNameResolver {

    private static final Map<Class<?>, SerializedLambda> LAMBDA_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, String> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    private LambdaFieldNameResolver() {

    }

    public static String resolveFieldName(Serializable lambda) {

        SerializedLambda serializedLambda = resolveSerializedLambda(lambda);
        String methodName = serializedLambda.getImplMethodName();
        String key = serializedLambda.getImplClass() + "#" + methodName;
        return FIELD_NAME_CACHE.computeIfAbsent(key, unused -> toFieldName(methodName));
    }

    private static SerializedLambda resolveSerializedLambda(Serializable lambda) {

        Class<?> lambdaClass = lambda.getClass();
        return LAMBDA_CACHE.computeIfAbsent(lambdaClass, unused -> {
            try {
                Method method = lambdaClass.getDeclaredMethod("writeReplace");
                method.setAccessible(true);
                return (SerializedLambda) method.invoke(lambda);
            } catch (Exception e) {
                throw new IllegalStateException("无法解析 Lambda", e);
            }
        });
    }

    private static String toFieldName(String methodName) {

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (!methodName.isEmpty() && Character.isLowerCase(methodName.charAt(0)) && !methodName.contains("$")) {
            return methodName;
        }
        throw new IllegalStateException("不是标准 getter/setter: " + methodName);
    }

    static void clearCacheForTest() {

        LAMBDA_CACHE.clear();
        FIELD_NAME_CACHE.clear();
    }

    static int lambdaCacheSizeForTest() {

        return LAMBDA_CACHE.size();
    }

    static int fieldNameCacheSizeForTest() {

        return FIELD_NAME_CACHE.size();
    }
}
