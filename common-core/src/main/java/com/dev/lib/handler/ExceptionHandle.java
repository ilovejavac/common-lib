package com.dev.lib.handler;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.MessageUtils;
import com.dev.lib.web.model.ServerResponse;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RestControllerAdvice
public class ExceptionHandle {

    // ==================== 业务异常 ====================

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public ServerResponse<Void> handleBizException(BizException e, HttpServletRequest request) {

        log.warn("业务异常", e);
        String message = e.getMsger();
        if (e.isI18n()) {
            message = MessageUtils.get(e.getMsger(), e.getArgs());
        }
        return ServerResponse.fail(e.getCoder(), message);
    }

    // ==================== 参数校验相关 ====================

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ServerResponse<Map<String, String>> handleMethodValidationException(HandlerMethodValidationException e) {

        log.warn("参数校验失败: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                String field = paramName != null ? paramName : "unknown";
                String message = error.getDefaultMessage() != null
                                 ? error.getDefaultMessage()
                                 : MessageUtils.get("error.validation.failed");
                errors.put(field, message);
            });
        }
        return ServerResponse.requestFail(StandardErrorCodes.VALIDATION_FAILED, MessageUtils.get("error.validation.failed"), errors);
    }

    /**
     * 参数校验异常 - @Validated
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ServerResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {

        log.warn("参数校验失败: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : MessageUtils.get("error.validation.failed")
            );
        }
        return ServerResponse.requestFail(StandardErrorCodes.VALIDATION_FAILED, MessageUtils.get("error.validation.failed"), errors);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ServerResponse<Map<String, String>> handleBindException(BindException e) {

        log.warn("参数绑定失败: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getFieldErrors()) {
            errors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : MessageUtils.get("error.bind.failed")
            );
        }
        return ServerResponse.requestFail(StandardErrorCodes.BIND_FAILED, MessageUtils.get("error.bind.failed"), errors);
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ServerResponse<List<String>> handleConstraintViolationException(ConstraintViolationException e) {

        log.warn("约束校验失败: {}", e.getMessage());
        List<String> errors = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();
        return ServerResponse.requestFail(StandardErrorCodes.VALIDATION_FAILED, MessageUtils.get("error.validation.failed"), errors);
    }

    /**
     * 请求参数缺失
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ServerResponse<Void> handleMissingParameter(MissingServletRequestParameterException e) {

        log.warn("缺少请求参数: {}", e.getParameterName());
        return ServerResponse.requestFail(StandardErrorCodes.PARAM_MISSING, MessageUtils.get("error.param.missing", e.getParameterName()), null);
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ServerResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {

        String typeName = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        log.warn("参数类型错误: {} 期望类型: {}", e.getName(), typeName);
        return ServerResponse.requestFail(StandardErrorCodes.PARAM_TYPE_INVALID, MessageUtils.get("error.param.type", e.getName(), typeName), null);
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ServerResponse<Void> handleIllegalArgument(IllegalArgumentException e) {

        log.warn("非法参数: {}", e.getMessage());
        return ServerResponse.requestFail(StandardErrorCodes.PARAM_INVALID, MessageUtils.get("error.param.invalid"), null);
    }

    // ==================== HTTP 相关 ====================

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ServerResponse<Void> handleMissingHeader(MissingRequestHeaderException e) {

        log.warn("缺少请求头", e);
        return ServerResponse.requestFail(StandardErrorCodes.HEADER_MISSING, MessageUtils.get("error.header.missing", e.getHeaderName()), null);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ServerResponse<Void> handleMissingPathVariable(MissingPathVariableException e) {

        log.warn("缺少路径变量", e);
        return ServerResponse.requestFail(StandardErrorCodes.PATH_VARIABLE_MISSING, MessageUtils.get("error.path.variable.missing", e.getVariableName()), null);
    }

    /**
     * 请求体无法读取
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ServerResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {

        log.warn("请求体格式错误", e);
        return ServerResponse.requestFail(StandardErrorCodes.REQUEST_BODY_INVALID, MessageUtils.get("error.request.body.invalid"), null);
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ServerResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {

        log.warn("不支持的请求方法", e);
        String supportedMethods = e.getSupportedHttpMethods() != null ? e.getSupportedHttpMethods().toString() : "";
        return ServerResponse.requestFail(
                StandardErrorCodes.METHOD_NOT_ALLOWED,
                MessageUtils.get("error.method.not.supported", e.getMethod(), supportedMethods),
                null
        );
    }

    /**
     * Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ServerResponse<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {

        log.warn("不支持的 Content-Type", e);
        return ServerResponse.requestFail(StandardErrorCodes.MEDIA_TYPE_UNSUPPORTED, MessageUtils.get("error.media.type.unsupported"), null);
    }

    /**
     * Accept 头不匹配
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ServerResponse<Void> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException e) {

        log.warn("无法生成客户端可接受的响应类型", e);
        return ServerResponse.requestFail(StandardErrorCodes.MEDIA_TYPE_NOT_ACCEPTABLE, MessageUtils.get("error.media.type.not.acceptable"), null);
    }

    /**
     * 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ServerResponse<Void> handleNotFound(NoHandlerFoundException e) {

        log.warn("接口不存在", e);
        return ServerResponse.requestFail(StandardErrorCodes.API_NOT_FOUND, MessageUtils.get("error.api.not.found"), null);
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ServerResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {

        log.warn("上传文件大小超限", e);
        return ServerResponse.requestFail(StandardErrorCodes.FILE_SIZE_EXCEEDED, MessageUtils.get("error.file.size.exceeded"), null);
    }

    /**
     * 异步请求超时
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ServerResponse<Void> handleAsyncTimeout(AsyncRequestTimeoutException e, HttpServletRequest request) {

        log.warn("异步请求超时 [{}]", request.getRequestURI());
        return ServerResponse.fail(StandardErrorCodes.REQUEST_TIMEOUT, MessageUtils.get("error.request.timeout"));
    }

    // ==================== 系统异常 ====================

    /**
     * 非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ServerResponse<Void> handleIllegalState(IllegalStateException e, HttpServletRequest request) {

        log.error("非法状态 [{}]: ", request.getRequestURI(), e);
        return ServerResponse.fail(StandardErrorCodes.SYSTEM_STATE_ERROR, MessageUtils.get("error.system.state"));
    }

    /**
     * 通用异常（兜底）
     */
    @ExceptionHandler(Throwable.class)
    public ServerResponse<Void> handleException(Throwable e, HttpServletRequest request) {

        log.error("系统异常 [{}]: ", request.getRequestURI(), e);
        return ServerResponse.fail(StandardErrorCodes.SYSTEM_ERROR, MessageUtils.get("error.system.contact"));
    }

}
