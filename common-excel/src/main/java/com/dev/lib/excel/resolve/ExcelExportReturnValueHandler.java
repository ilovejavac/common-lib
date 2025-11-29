package com.dev.lib.excel.resolve;

import cn.idev.excel.EasyExcel;
import com.dev.lib.excel.ExcelExport;
import com.dev.lib.excel.ExcelUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * 处理 @ExcelExport 注解的返回值
 */
public class ExcelExportReturnValueHandler implements HandlerMethodReturnValueHandler {
    
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    
    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.hasMethodAnnotation(ExcelExport.class)
            && Collection.class.isAssignableFrom(returnType.getParameterType());
    }
    
    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
                                  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        mavContainer.setRequestHandled(true);
        
        if (returnValue == null) {
            return;
        }
        
        ExcelExport annotation = returnType.getMethodAnnotation(ExcelExport.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        
        if (response == null) {
            throw new IllegalStateException("HttpServletResponse 不可用");
        }
        
        String fileName = ExcelUtils.resolveFileName(annotation.fileName());
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedName + ".xlsx");
        
        Class<?> dataClass = ExcelUtils.extractReturnGenericType(returnType);
        EasyExcel.write(response.getOutputStream(), dataClass)
            .sheet(annotation.sheetName())
            .doWrite((Collection<?>) returnValue);
    }
}