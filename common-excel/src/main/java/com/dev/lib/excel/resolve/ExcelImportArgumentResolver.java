package com.dev.lib.excel.resolve;

import cn.idev.excel.FastExcelFactory;
import com.dev.lib.excel.ExcelData;
import com.dev.lib.excel.ExcelException;
import com.dev.lib.excel.ExcelImport;
import com.dev.lib.excel.ExcelUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

/**
 * 解析 @ExcelData 参数
 */
public class ExcelImportArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExcelData.class)
                && parameter.hasMethodAnnotation(ExcelImport.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory
    ) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (!(request instanceof MultipartRequest multipartRequest)) {
            throw new ExcelException("请求必须是 multipart 类型");
        }

        ExcelImport annotation = parameter.getMethodAnnotation(ExcelImport.class);
        String fileParam = annotation != null ? annotation.fileParam() : "file";
        MultipartFile file = multipartRequest.getFile(fileParam);

        if (file == null || file.isEmpty()) {
            throw new ExcelException("未找到上传文件或文件为空，参数名: " + fileParam);
        }

        Class<?> dataClass = ExcelUtils.extractGenericType(parameter);
        return FastExcelFactory.read(file.getInputStream(), dataClass, null)
                .sheet()
                .doReadSync();
    }
}