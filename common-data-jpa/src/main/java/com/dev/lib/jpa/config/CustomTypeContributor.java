package com.dev.lib.jpa.config;

import com.dev.lib.jpa.entity.type.*;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

/**
 * Hibernate 类型配置贡献器
 * <p>
 * 通过 SPI 自动注册，修改各类型的默认配置：
 * - String: varchar(500)
 * - BigDecimal: decimal(19,4)
 * - BigInteger: numeric(19)
 * - Double: precision(17)
 * - Float: precision(8)
 * - byte[]: varbinary(4000)
 * - 时间类型: precision(6) 微秒级
 * - Duration: numeric(21,9)
 */
public class CustomTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        // 字符串类型
        typeContributions.contributeJavaType(CustomStringJavaType.INSTANCE);

        // 数值类型
        typeContributions.contributeJavaType(CustomBigDecimalJavaType.INSTANCE);
        typeContributions.contributeJavaType(CustomBigIntegerJavaType.INSTANCE);

        // 时间类型
        typeContributions.contributeJavaType(CustomInstantJavaType.INSTANCE);
        typeContributions.contributeJavaType(CustomLocalDateTimeJavaType.INSTANCE);
    }

}