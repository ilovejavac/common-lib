package com.dev.lib.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.dev.lib.web.sensitive.Sensitive;
import com.dev.lib.web.sensitive.SensitiveType;
import com.dev.lib.web.serialize.PopulateContextHolder;
import com.dev.lib.web.serialize.PopulateField;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import java.beans.Introspector;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;

public final class JacksonSupport {

    public static final String TIME_ZONE = "Asia/Shanghai";

    public static final ZoneId ZONE_ID = ZoneId.of(TIME_ZONE);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final long JS_SAFE_INTEGER_MAX = 9007199254740991L;

    private static final JsonMapper MAPPER = createMapper();

    private JacksonSupport() {
    }

    public static void customize(JsonMapper.Builder builder) {

        builder
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING, EnumFeature.READ_ENUMS_USING_TO_STRING)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .defaultTimeZone(TimeZone.getTimeZone(ZONE_ID))
                .annotationIntrospector(new SmartMatchAnnotationIntrospector())
                .changeDefaultPropertyInclusion(inclusion ->
                        inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .addModule(commonModule());
    }

    public static JsonMapper mapper() {

        return MAPPER;
    }

    private static JsonMapper createMapper() {

        JsonMapper.Builder builder = JsonMapper.builder();
        customize(builder);
        return builder.build();
    }

    private static SimpleModule commonModule() {

        SimpleModule module = new SimpleModule("common-lib-jackson");
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        LongSerializer longSerializer = new LongSerializer();
        module.addSerializer(Long.class, longSerializer);
        module.addSerializer(Long.TYPE, longSerializer);
        module.addSerializer(LocalDateTime.class, new FormattedSerializer<>(
                LocalDateTime.class,
                DATE_TIME_FORMATTER::format
        ));
        module.addDeserializer(LocalDateTime.class, new StringValueDeserializer<>(
                LocalDateTime.class,
                JacksonSupport::parseLocalDateTime
        ));
        module.addSerializer(LocalDate.class, new FormattedSerializer<>(LocalDate.class, DATE_FORMATTER::format));
        module.addDeserializer(LocalDate.class, new StringValueDeserializer<>(LocalDate.class, JacksonSupport::parseLocalDate));
        module.addSerializer(LocalTime.class, new FormattedSerializer<>(LocalTime.class, TIME_FORMATTER::format));
        module.addDeserializer(LocalTime.class, new StringValueDeserializer<>(LocalTime.class, JacksonSupport::parseLocalTime));
        module.addSerializer(Instant.class, new FormattedSerializer<>(Instant.class, value ->
                DATE_TIME_FORMATTER.format(value.atZone(ZONE_ID))));
        module.addDeserializer(Instant.class, new StringValueDeserializer<>(Instant.class, JacksonSupport::parseInstant));
        module.setSerializerModifier(new CommonValueSerializerModifier());
        return module;
    }

    private static LocalDateTime parseLocalDateTime(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        return value.contains("T")
               ? LocalDateTime.parse(value)
               : LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private static LocalDate parseLocalDate(String text) {

        return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DATE_FORMATTER);
    }

    private static LocalTime parseLocalTime(String text) {

        return text == null || text.isBlank() ? null : LocalTime.parse(text.trim(), TIME_FORMATTER);
    }

    private static Instant parseInstant(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("Date string too long: " + value.length());
        }
        return value.contains("T")
               ? Instant.parse(value)
               : LocalDateTime.parse(value, DATE_TIME_FORMATTER).atZone(ZONE_ID).toInstant();
    }

    private static String mask(String value, SensitiveType type) {

        if (value.isEmpty()) {
            return value;
        }
        return switch (type) {
            case PHONE -> value.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
            case ID_CARD -> value.replaceAll("(\\d{3})\\d{11}(\\d{4})", "$1***********$2");
            case EMAIL -> value.replaceAll("(\\w{1})\\w+(@.*)", "$1***$2");
            case NAME -> value.charAt(0) + "*".repeat(value.length() - 1);
            case BANK_CARD -> value.replaceAll("(\\d{4})\\d+(\\d{4})", "$1 **** **** $2");
        };
    }

    private static String logicalPropertyName(String memberName) {

        if (memberName.startsWith("get") && memberName.length() > 3) {
            return Introspector.decapitalize(memberName.substring(3));
        }
        if (memberName.startsWith("is") && memberName.length() > 2) {
            return Introspector.decapitalize(memberName.substring(2));
        }
        if (memberName.startsWith("set") && memberName.length() > 3) {
            return Introspector.decapitalize(memberName.substring(3));
        }
        return memberName;
    }

    private static final class BigDecimalSerializer extends StdScalarSerializer<BigDecimal> {

        private BigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) throws JacksonException {

            generator.writeNumber(value.setScale(6, RoundingMode.HALF_UP));
        }
    }

    private static final class LongSerializer extends StdScalarSerializer<Long> {

        private LongSerializer() {
            super(Long.class);
        }

        @Override
        public void serialize(Long value, JsonGenerator generator, SerializationContext context) throws JacksonException {

            if (value > JS_SAFE_INTEGER_MAX || value < -JS_SAFE_INTEGER_MAX) {
                generator.writeString(value.toString());
            } else {
                generator.writeNumber(value);
            }
        }
    }

    private static final class FormattedSerializer<T> extends StdScalarSerializer<T> {

        private final Function<T, String> formatter;

        private FormattedSerializer(Class<T> type, Function<T, String> formatter) {
            super(type);
            this.formatter = formatter;
        }

        @Override
        public void serialize(T value, JsonGenerator generator, SerializationContext context) throws JacksonException {

            generator.writeString(formatter.apply(value));
        }
    }

    private static final class StringValueDeserializer<T> extends FromStringDeserializer<T> {

        private final Function<String, T> parser;

        private StringValueDeserializer(Class<T> type, Function<String, T> parser) {
            super(type);
            this.parser = parser;
        }

        @Override
        protected T _deserialize(String value, DeserializationContext context) {

            return parser.apply(value);
        }
    }

    private static final class SensitiveStringSerializer extends ValueSerializer<Object> {

        private final SensitiveType type;

        private SensitiveStringSerializer(SensitiveType type) {
            this.type = type;
        }

        @Override
        public void serialize(Object value, JsonGenerator generator, SerializationContext context) throws JacksonException {

            generator.writeString(mask((String) value, type));
        }
    }

    private static final class CommonValueSerializerModifier extends ValueSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(
                SerializationConfig config,
                BeanDescription.Supplier beanDescription,
                List<BeanPropertyWriter> properties
        ) {

            List<BeanPropertyWriter> populatedProperties = new ArrayList<>();
            for (BeanPropertyWriter property : properties) {
                Sensitive sensitive = property.getAnnotation(Sensitive.class);
                if (sensitive != null && property.getType().getRawClass() == String.class) {
                    property.assignSerializer(new SensitiveStringSerializer(sensitive.type()));
                }
                PopulateField populateField = property.getAnnotation(PopulateField.class);
                if (populateField != null) {
                    populatedProperties.add(new PopulatePropertyWriter(property, populateField));
                }
            }
            properties.addAll(populatedProperties);
            return properties;
        }
    }

    private static final class PopulatePropertyWriter extends BeanPropertyWriter {

        private final BeanPropertyWriter source;

        private final String loaderName;

        private PopulatePropertyWriter(BeanPropertyWriter source, PopulateField populateField) {
            super(source, PropertyName.construct(sourcePropertyName(source) + populateField.suffix()));
            this.source = source;
            this.loaderName = populateField.loader();
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {

            Object key = source.get(bean);
            if (key == null) {
                return;
            }
            Object value = PopulateContextHolder.get(loaderName, key);
            if (value != null) {
                context.defaultSerializeProperty(getName(), value, generator);
            }
        }

        private static String sourcePropertyName(BeanPropertyWriter source) {

            AnnotatedMember member = source.getMember();
            return member == null ? source.getName() : logicalPropertyName(member.getName());
        }
    }

    private static final class SmartMatchAnnotationIntrospector extends JacksonAnnotationIntrospector {

        @Override
        public List<PropertyName> findPropertyAliases(MapperConfig<?> config, Annotated annotated) {

            List<PropertyName> declaredAliases = super.findPropertyAliases(config, annotated);
            Set<PropertyName> aliases = declaredAliases == null
                                        ? new LinkedHashSet<>()
                                        : new LinkedHashSet<>(declaredAliases);
            if (annotated instanceof AnnotatedMember) {
                String logicalName = logicalPropertyName(annotated.getName());
                aliases.add(PropertyName.construct(logicalName));
                aliases.add(PropertyName.construct(PropertyNamingStrategies.SNAKE_CASE.nameForField(config, null, logicalName)));
                aliases.add(PropertyName.construct(PropertyNamingStrategies.KEBAB_CASE.nameForField(config, null, logicalName)));
            }
            return List.copyOf(aliases);
        }
    }

}
