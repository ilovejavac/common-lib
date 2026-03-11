package com.dev.lib.config;

import com.dev.lib.web.serialize.PopulateContextHolder;
import com.dev.lib.web.serialize.PopulateField;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.VirtualAnnotatedMember;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PopulateFieldBeanSerializerModifier extends BeanSerializerModifier {

    private static final Set<String> EXCLUDE_FIELDS = Set.of("reversion", "deleted");

    private static final Map<Class<?>, FieldMeta[]> CACHE = new ConcurrentHashMap<>();

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties
    ) {

        List<BeanPropertyWriter> writers = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            if (!EXCLUDE_FIELDS.contains(writer.getName())) {
                writers.add(writer);
            }
        }

        for (FieldMeta meta : CACHE.computeIfAbsent(beanDesc.getBeanClass(), this::parseFields)) {
            writers.add(buildPopulateWriter(config, beanDesc, meta));
        }
        return writers;
    }

    public SimpleModule asModule() {

        SimpleModule module = new SimpleModule("common-lib-web-jackson");
        module.setSerializerModifier(this);
        return module;
    }

    private BeanPropertyWriter buildPopulateWriter(
            SerializationConfig config,
            BeanDescription beanDesc,
            FieldMeta meta
    ) {

        JavaType declaredType = config.constructType(Object.class);
        VirtualAnnotatedMember member = new VirtualAnnotatedMember(
                beanDesc.getClassInfo(),
                beanDesc.getBeanClass(),
                meta.outputName(),
                declaredType
        );
        BeanPropertyDefinition definition = SimpleBeanPropertyDefinition.construct(
                config,
                member,
                PropertyName.construct(meta.outputName())
        );
        return new PopulateFieldWriter(
                definition,
                beanDesc.getClassAnnotations(),
                declaredType,
                meta.field(),
                meta.loaderName()
        );
    }

    private FieldMeta[] parseFields(Class<?> clazz) {

        return java.util.Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(PopulateField.class))
                .map(field -> {
                    PopulateField ann = field.getAnnotation(PopulateField.class);
                    ReflectionUtils.makeAccessible(field);
                    return new FieldMeta(field, ann.loader(), field.getName() + ann.suffix());
                })
                .toArray(FieldMeta[]::new);
    }

    private record FieldMeta(Field field, String loaderName, String outputName) {
    }

    static final class PopulateFieldWriter extends VirtualBeanPropertyWriter {

        private final transient Field field;

        private final String loaderName;

        PopulateFieldWriter(
                BeanPropertyDefinition propDef,
                Annotations contextAnnotations,
                JavaType declaredType,
                Field field,
                String loaderName
        ) {

            super(propDef, contextAnnotations, declaredType);
            this.field = field;
            this.loaderName = loaderName;
        }

        @SuppressWarnings("unused")
        PopulateFieldWriter() {

            field = null;
            loaderName = null;
        }

        @Override
        protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {

            if (field == null) {
                return null;
            }
            Object idValue = field.get(bean);
            return idValue != null ? PopulateContextHolder.get(loaderName, idValue) : null;
        }

        @Override
        public VirtualBeanPropertyWriter withConfig(
                MapperConfig<?> config,
                AnnotatedClass declaringClass,
                BeanPropertyDefinition propDef,
                JavaType type
        ) {

            Field resolvedField = ReflectionUtils.findField(
                    declaringClass.getRawType(),
                    propDef.getInternalName().replaceFirst("Info$", "")
            );
            if (resolvedField != null) {
                ReflectionUtils.makeAccessible(resolvedField);
            }
            return new PopulateFieldWriter(
                    propDef,
                    declaringClass.getAnnotations(),
                    type,
                    resolvedField,
                    loaderName
            );
        }
    }
}
