package com.dev.lib.handler;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.MessageUtils;
import com.dev.lib.web.model.ServerResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RestControllerAdvice
public class ExceptionHandle {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandle.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public ServerResponse<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {}: {}", request.getRequestURI(), e.getCoder(), e.getMsger(), e);

        String message = e.getMsger();
        if (e.isI18n()) {
            message = MessageUtils.get(e.getMsger(), e.getArgs());
        }

        return ServerResponse.fail(e.getCoder(), message);
    }

    /**
     * 参数校验异常 - @Validated
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : MessageUtils.get(
                            "error.validation.failed")
            );
        }

        return ServerResponse.fail(400, MessageUtils.get("error.validation.failed"), errors);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<Map<String, String>> handleBindException(BindException e) {
        log.warn("参数绑定失败: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getFieldErrors()) {
            errors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : MessageUtils.get(
                            "error.bind.failed")
            );
        }

        return ServerResponse.fail(400, MessageUtils.get("error.bind.failed"), errors);
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<List<String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束校验失败: {}", e.getMessage());

        List<String> errors = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();

        return ServerResponse.fail(400, MessageUtils.get("error.validation.failed"), errors);
    }

    /**
     * 请求参数缺失
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ServerResponse.fail(400, MessageUtils.get("error.param.missing", e.getParameterName()));
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String typeName = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        log.warn("参数类型错误: {} 期望类型: {}", e.getName(), typeName);
        return ServerResponse.fail(400, MessageUtils.get("error.param.type", e.getName(), typeName));
    }

    /**
     * 请求体无法读取
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return ServerResponse.fail(400, MessageUtils.get("error.request.body.invalid"));
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ServerResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        String supportedMethods = e.getSupportedHttpMethods() != null ? e.getSupportedHttpMethods().toString() : "";
        return ServerResponse.fail(
                405,
                MessageUtils.get("error.method.not.supported", e.getMethod(), supportedMethods)
        );
    }

    /**
     * 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ServerResponse<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return ServerResponse.fail(404, MessageUtils.get("error.api.not.found"));
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ServerResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超限: {}", e.getMessage());
        return ServerResponse.fail(413, MessageUtils.get("error.file.size.exceeded"));
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServerResponse<Void> handleNullPointer(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 [{}]: ", request.getRequestURI(), e);
        return ServerResponse.fail(500, MessageUtils.get("error.system"));
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServerResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ServerResponse.fail(
                400,
                e.getMessage() != null ? e.getMessage() : MessageUtils.get("error.param.invalid")
        );
    }

    /**
     * 非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServerResponse<Void> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.error("非法状态 [{}]: ", request.getRequestURI(), e);
        return ServerResponse.fail(500, MessageUtils.get("error.system.state"));
    }

    /**
     * 通用异常（兜底）
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServerResponse<Void> handleException(Throwable e, HttpServletRequest request) {
        log.error("系统异常 [{}]: ", request.getRequestURI(), e);

        String message =
                isProductionEnvironment() ? MessageUtils.get("error.system.contact") : (e.getMessage() != null ? e.getMessage() : MessageUtils.get(
                        "error.unknown"));

        return ServerResponse.fail(500, message);
    }

    private boolean isProductionEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active");
        return activeProfile != null && activeProfile.contains("prod");
    }
}