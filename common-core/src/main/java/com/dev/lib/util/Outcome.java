package com.dev.lib.util;

import com.dev.lib.exceptions.BizException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 业务操作结果封装 (同步模式)
 * 替代 boolean 或 void，支持链式调用 .then().fail()
 *
 * @param <T> 成功时的数据类型
 */
public final class Outcome<T> {

    private final T data;

    private final String errorMessage;

    private final boolean success;

    // 私有构造，强制使用静态工厂
    private Outcome(T data, String errorMessage, boolean success) {

        this.data = data;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    // ==================== 1. 静态构造方法 ====================

    /**
     * 成功，带返回值
     */
    public static <T> Outcome<T> success(T data) {

        return new Outcome<>(data, null, true);
    }

    /**
     * 成功，无返回值 (用于 void 场景)
     */
    public static <T> Outcome<T> success() {

        return new Outcome<>(null, null, true);
    }

    /**
     * 失败，带错误信息
     */
    public static <T> Outcome<T> failure(String message) {

        return new Outcome<>(null, message, false);
    }

    /**
     * 失败，带异常 (自动提取 msg)
     */
    public static <T> Outcome<T> failure(Throwable ex) {

        return new Outcome<>(null, ex != null ? ex.getMessage() : "Unknown Error", false);
    }

    /**
     * 尝试执行，自动捕获异常
     */
    public static <T> Outcome<T> call(Supplier<T> supplier) {

        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    // ==================== 2. 核心链式调用 ====================

    /**
     * 成功时执行 (类似 Promise.then)
     */
    public Outcome<T> then(Consumer<T> action) {

        if (success) {
            action.accept(data);
        }
        return this;
    }

    /**
     * 失败时执行 (类似 Promise.catch/fail)
     */
    public Outcome<T> fail(Consumer<String> action) {

        if (!success) {
            action.accept(errorMessage);
        }
        return this;
    }

    /**
     * 无论成功失败都执行 (类似 finally)
     */
    public Outcome<T> always(Runnable action) {

        action.run();
        return this;
    }

    // ==================== 3. 数据转换 ====================

    /**
     * 转换数据 (如果当前是失败，则直接传递失败)
     * 例: outcome.map(User::getId)
     */
    public <R> Outcome<R> map(Function<T, R> mapper) {

        if (!success) {
            return Outcome.failure(errorMessage);
        }
        try {
            return Outcome.success(mapper.apply(data));
        } catch (Exception e) {
            return Outcome.failure(e);
        }
    }

    /**
     * 扁平转换 (用于 mapper 本身也返回 Outcome 的情况)
     */
    public <R> Outcome<R> flatMap(Function<T, Outcome<R>> mapper) {

        if (!success) {
            return Outcome.failure(errorMessage);
        }
        return mapper.apply(data);
    }

    // ==================== 4. 终结取值 ====================

    /**
     * 获取值，如果失败则返回默认值
     */
    public T orElse(T other) {

        return success ? data : other;
    }

    /**
     * 获取值，如果失败则抛出指定异常
     */
    public <X extends BizException> T getOrThrow(Function<String, X> exceptionSupplier) throws X {

        if (!success) {
            throw exceptionSupplier.apply(errorMessage);
        }
        return data;
    }

    /**
     * 从 Optional 转换
     *
     * @param optional 源 Optional
     * @param emptyMsg 如果 Optional 为空，返回的错误信息
     */
    public static <T> Outcome<T> of(Optional<T> optional, String emptyMsg) {

        return optional.map(Outcome::success)
                .orElseGet(() -> Outcome.failure(emptyMsg));
    }

    /**
     * 从可能为 null 的值转换 (语法糖，省去包装 Optional)
     *
     * @param value   可能为 null 的值
     * @param nullMsg 如果值为 null，返回的错误信息
     */
    public static <T> Outcome<T> of(T value, String nullMsg) {

        return value != null ? success(value) : failure(nullMsg);
    }

    // ==================== 5. 状态判断 ====================

    public boolean isSuccess() {

        return success;
    }

    public boolean isFailure() {

        return !success;
    }

    public String getErrorMessage() {

        return errorMessage;
    }

}
