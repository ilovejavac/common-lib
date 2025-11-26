package com.dev.lib.jpa.entity.type;

/**
 * 类型默认值配置
 * <p>
 * 修改这里的常量即可全局生效
 */
public final class TypeDefaults {

    private TypeDefaults() {
    }

    // ==================== 字符串类型 ====================

    /**
     * String -> varchar(?) 默认长度
     */
    public static final long STRING_LENGTH = 500L;

    // ==================== 数值类型 ====================

    /**
     * BigDecimal 总位数
     */
    public static final int DECIMAL_PRECISION = 19;

    /**
     * BigDecimal 小数位数
     */
    public static final int DECIMAL_SCALE = 4;

    /**
     * BigInteger 总位数
     */
    public static final int BIG_INTEGER_PRECISION = 20;

    /**
     * Double 精度
     */
    public static final int DOUBLE_PRECISION = 17;

    /**
     * Float 精度
     */
    public static final int FLOAT_PRECISION = 8;

    // ==================== 二进制类型 ====================

    /**
     * byte[] -> varbinary(?) 默认长度
     */
    public static final long BYTE_ARRAY_LENGTH = 4000L;

    // ==================== 时间类型 ====================

    /**
     * 时间戳精度（秒的小数位数）
     * 0 = 秒级 (2024-01-01 12:00:00)
     * 3 = 毫秒级 (2024-01-01 12:00:00.123)
     * 6 = 微秒级 (2024-01-01 12:00:00.123456)
     * 9 = 纳秒级 (2024-01-01 12:00:00.123456789)
     */
    public static final int TIMESTAMP_PRECISION = 6;

    /**
     * Duration 总位数
     */
    public static final int DURATION_PRECISION = 21;

    /**
     * Duration 小数位数
     */
    public static final int DURATION_SCALE = 9;
}