package com.dev.lib.jpa.multiple;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 {@link AnnotationRepositoryConfigurationSource} 的极简实现，
 * 通过合成 {@link AnnotationMetadata} 将包路径、EMF 引用、TM 引用传入 Spring Data 框架。
 *
 * <p>继承 {@code AnnotationRepositoryConfigurationSource} 而非直接实现接口，
 * 是因为 {@link org.springframework.data.repository.config.RepositoryConfigurationDelegate}
 * 硬编码了类型校验，只接受 Annotation 或 Xml 两种来源。
 */
class SimpleRepositoryConfigurationSource extends AnnotationRepositoryConfigurationSource {

    SimpleRepositoryConfigurationSource(String[] packages,
                                        String entityManagerFactoryRef,
                                        String transactionManagerRef,
                                        ResourceLoader resourceLoader,
                                        Environment environment,
                                        BeanDefinitionRegistry registry) {

        super(
                buildMetadata(packages, entityManagerFactoryRef, transactionManagerRef),
                EnableJpaRepositories.class,
                resourceLoader,
                environment,
                registry,
                (BeanNameGenerator) null
        );
    }

    // ==================== 合成 AnnotationMetadata ====================

    private static AnnotationMetadata buildMetadata(String[] packages,
                                                    String entityManagerFactoryRef,
                                                    String transactionManagerRef) {

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value",                           new String[0]);
        attrs.put("basePackages",                    packages);
        attrs.put("basePackageClasses",              new Class<?>[0]);
        attrs.put("includeFilters",                  new org.springframework.core.annotation.AnnotationAttributes[0]);
        attrs.put("excludeFilters",                  new org.springframework.core.annotation.AnnotationAttributes[0]);
        attrs.put("repositoryImplementationPostfix", "Impl");
        attrs.put("namedQueriesLocation",            "");
        attrs.put("queryLookupStrategy",             QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND);
        attrs.put("repositoryFactoryBeanClass",      JpaRepositoryFactoryBean.class);
        attrs.put("repositoryBaseClass",             DefaultRepositoryBaseClass.class);
        attrs.put("entityManagerFactoryRef",         entityManagerFactoryRef);
        attrs.put("transactionManagerRef",           transactionManagerRef);
        attrs.put("considerNestedRepositories",      false);
        attrs.put("enableDefaultTransactions",       true);
        attrs.put("bootstrapMode",                   BootstrapMode.DEFAULT);
        attrs.put("escapeCharacter",                 '\\');
        attrs.put("nameGenerator",                   BeanNameGenerator.class);
        attrs.put("queryEnhancerSelector",           QueryEnhancerSelector.DefaultQueryEnhancerSelector.class);

        return new SyntheticEnableJpaRepositoriesMetadata(attrs);
    }

    // ==================== 合成 AnnotationMetadata 实现 ====================

    /**
     * 伪装成"被 @EnableJpaRepositories 标注的虚拟类"的 AnnotationMetadata。
     * AnnotationRepositoryConfigurationSource 构造时调用
     * metadata.getAnnotationAttributes(EnableJpaRepositories.class.getName()) 读取属性。
     */
    private static class SyntheticEnableJpaRepositoriesMetadata implements AnnotationMetadata {

        private static final String ANNOTATION_NAME = EnableJpaRepositories.class.getName();
        // 伪造一个虚拟类名，保证 getClassName() 不为 null
        private static final String FAKE_CLASS_NAME  = "com.dev.lib.jpa.multiple.SyntheticJpaConfig";

        private final Map<String, Object> attrs;

        SyntheticEnableJpaRepositoriesMetadata(Map<String, Object> attrs) {
            this.attrs = attrs;
        }

        // AnnotationRepositoryConfigurationSource 核心读取路径
        @Override
        public Map<String, Object> getAnnotationAttributes(String annotationName) {
            if (ANNOTATION_NAME.equals(annotationName)) return attrs;
            return null;
        }

        @Override
        public Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
            return getAnnotationAttributes(annotationName);
        }

        @Override
        public boolean hasAnnotation(String annotationName) {
            return ANNOTATION_NAME.equals(annotationName);
        }

        @Override
        public boolean hasMetaAnnotation(String metaAnnotationName) {
            return false;
        }

        @Override
        public boolean isAnnotated(String annotationName) {
            return ANNOTATION_NAME.equals(annotationName);
        }

        @Override
        public String getClassName() {
            return FAKE_CLASS_NAME;
        }

        @Override
        public boolean isInterface() { return false; }

        @Override
        public boolean isAnnotation() { return false; }

        @Override
        public boolean isAbstract() { return false; }

        @Override
        public boolean isFinal() { return false; }

        @Override
        public boolean isIndependent() { return true; }

        @Override
        public String getEnclosingClassName() { return null; }

        @Override
        public String getSuperClassName() { return Object.class.getName(); }

        @Override
        public String[] getInterfaceNames() { return new String[0]; }

        @Override
        public String[] getMemberClassNames() { return new String[0]; }

        @Override
        public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
            return Set.of();
        }

        @Override
        public Set<MethodMetadata> getDeclaredMethods() {
            return Set.of();
        }

        @Override
        public MergedAnnotations getAnnotations() {
            return MergedAnnotations.of(java.util.Collections.emptyList());
        }

        @Override
        public Set<String> getAnnotationTypes() {
            return Set.of(ANNOTATION_NAME);
        }

        @Override
        public Set<String> getMetaAnnotationTypes(String annotationName) {
            return Set.of();
        }

        @Override
        public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
            return null;
        }

        @Override
        public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName,
                                                                        boolean classValuesAsString) {
            return null;
        }
    }
}
