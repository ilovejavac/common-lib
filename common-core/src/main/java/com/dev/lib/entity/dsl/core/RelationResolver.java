package com.dev.lib.entity.dsl.core;

/**
 * 关联关系解析器接口
 * <p>
 * core 模块定义接口，JPA 模块提供实现
 */
public interface RelationResolver {

    /**
     * 解析实体类的关联关系
     *
     * @param entityClass 实体类
     * @param fieldName   关联字段名
     * @return 关联信息，不存在返回 null
     */
    RelationInfo resolve(Class<?> entityClass, String fieldName);

    /**
     * 静态持有者，用于注册和获取实现
     */
    class Holder {

        private static volatile RelationResolver instance;

        public static void register(RelationResolver resolver) {

            instance = resolver;
        }

        public static RelationResolver get() {

            if (instance == null) {
                throw new IllegalStateException("RelationResolver 未注册，请确保 JPA 模块已初始化");
            }
            return instance;
        }

        public static boolean isRegistered() {

            return instance != null;
        }

    }

}