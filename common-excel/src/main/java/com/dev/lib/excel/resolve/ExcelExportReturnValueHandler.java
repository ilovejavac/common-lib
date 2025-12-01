package com.dev.lib.excel.resolve;

import cn.idev.excel.FastExcelFactory;
import com.dev.lib.excel.ExcelExport;
import com.dev.lib.excel.ExcelUtils;
import com.dev.lib.excel.config.AppExcelProperties;
import com.dev.lib.excel.config.ExcelLoadAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

/**
 * 处理 @ExcelExport 注解的返回值
 */
@Slf4j
@RequiredArgsConstructor
public class ExcelExportReturnValueHandler implements HandlerMethodReturnValueHandler {
    private final AppExcelProperties appExcelProperties;
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.hasMethodAnnotation(ExcelExport.class)
                && Collection.class.isAssignableFrom(returnType.getParameterType());
    }
    @Override
    public void handleReturnValue(
            Object returnValue, MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest
    ) throws Exception {
        // 标记请求已处理，避免后续视图解析
        mavContainer.setRequestHandled(true);

        // 返回值为空时直接返回
        if (returnValue == null) {
            return;
        }

        // 获取注解和响应对象
        ExcelExport annotation = returnType.getMethodAnnotation(ExcelExport.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) {
            throw new IllegalStateException("HttpServletResponse 不可用");
        }

        // 1. 解析请求头中的Excel加载方式
        ExcelLoadAction loadAction = resolveExcelLoadAction(webRequest);

        // 2. 处理文件名编码
        String fileName = ExcelUtils.resolveFileName(annotation.fileName());
        // UTF-8编码，替换+为%20避免空格问题
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        // 3. 设置基础响应头
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 4. 根据加载方式动态设置Content-Disposition
        String dispositionType = loadAction == ExcelLoadAction.STREAM ? "inline" : "attachment";
        response.setHeader(
                "Content-Disposition",
                String.format("%s;filename*=utf-8''%s.xlsx", dispositionType, encodedName)
        );

        // 5. 写入Excel数据到响应流
        Class<?> dataClass = ExcelUtils.extractReturnGenericType(returnType);
        FastExcelFactory.write(response.getOutputStream(), dataClass)
                .sheet(annotation.sheetName())
                .doWrite((Collection<?>) returnValue);
    }
    /**
     * 解析请求头中的Excel加载方式
     *
     * @param webRequest NativeWebRequest
     * @return ExcelLoadAction（默认DOWNLOAD）
     */
    private ExcelLoadAction resolveExcelLoadAction(NativeWebRequest webRequest) {
        // 默认下载模式
        ExcelLoadAction defaultAction =
                Optional.ofNullable(appExcelProperties.getLoad()).orElse(ExcelLoadAction.DOWNLOAD);
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (request == null) {
            log.warn("获取HttpServletRequest失败，使用默认Excel加载方式: {}", defaultAction);
            return defaultAction;
        }

        // 从请求头获取值
        String actionStr = request.getHeader(appExcelProperties.getExcelLoadHeader());
        if (actionStr == null || actionStr.isBlank()) {
            return defaultAction;
        }

        // 解析枚举（兼容大小写）
        try {
            return ExcelLoadAction.valueOf(actionStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的ExcelLoadAction请求头值: {}，使用默认值: {}", actionStr, defaultAction, e);
            return defaultAction;
        }
    }
}