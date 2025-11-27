package com.dev.lib.web.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 修改 Bean 序列化行为：
 * 对 @PopulateField 标记的字段，额外输出一个 {fieldName}{suffix} 字段
 */
@Component
public class PopulateFieldSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                      BeanDescription beanDesc,
                                                      List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> result = new ArrayList<>();

        for (BeanPropertyWriter writer : beanProperties) {
            result.add(writer);  // 保留原字段

            // 检查是否有 @PopulateField 注解
            PopulateField annotation = writer.getAnnotation(PopulateField.class);
            if (annotation != null) {
                // 添加额外的 Info 字段
                BeanPropertyWriter infoWriter = new PopulateInfoPropertyWriter(
                        writer,
                        annotation.loader(),
                        annotation.suffix()
                );
                result.add(infoWriter);
            }
        }

        return result;
    }

    /**
     * 虚拟属性 Writer：输出填充后的对象
     */
    private static class PopulateInfoPropertyWriter extends BeanPropertyWriter {
        private final BeanPropertyWriter delegate;
        private final String loaderName;
        private final String suffix;

        public PopulateInfoPropertyWriter(BeanPropertyWriter delegate, String loaderName, String suffix) {
            super(delegate);
            this.delegate = delegate;
            this.loaderName = loaderName;
            this.suffix = suffix;
        }

        @Override
        public String getName() {
            return delegate.getName() + suffix;
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            // 获取原字段值（ID）
            Object idValue;
            try {
                idValue = delegate.get(bean);
            } catch (Exception e) {
                return;
            }

            // 输出 Info 字段
            gen.writeFieldName(getName());

            if (idValue == null) {
                gen.writeNull();
                return;
            }

            // 从缓存获取填充对象
            Object populated = PopulateContextHolder.get(loaderName, idValue);
            if (populated != null) {
                gen.writeObject(populated);
            } else {
                gen.writeNull();
            }
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            serializeAsField(bean, gen, prov);
        }
    }
}