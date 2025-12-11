package com.dev.lib.excel.config;

import com.dev.lib.excel.resolve.ExcelExportReturnValueHandler;
import com.dev.lib.excel.resolve.ExcelImportArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 自动配置
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExcelAutoConfiguration implements InitializingBean {

    private final AppExcelProperties           appExcelProperties;

    private final RequestMappingHandlerAdapter handlerAdapter;

    @Override
    public void afterPropertiesSet() throws Exception {

        configureReturnValueHandlers();
        configureArgumentResolvers();
    }

    private void configureReturnValueHandlers() {

        List<HandlerMethodReturnValueHandler> handlers = handlerAdapter.getReturnValueHandlers();
        if (handlers != null) {
            List<HandlerMethodReturnValueHandler> newHandlers = new ArrayList<>(handlers.size() + 1);
            newHandlers.add(new ExcelExportReturnValueHandler(appExcelProperties));
            newHandlers.addAll(handlers);
            handlerAdapter.setReturnValueHandlers(newHandlers);
        }
    }

    private void configureArgumentResolvers() {

        List<HandlerMethodArgumentResolver> resolvers = handlerAdapter.getArgumentResolvers();
        if (resolvers != null) {
            List<HandlerMethodArgumentResolver> newResolvers = new ArrayList<>(resolvers.size() + 1);
            newResolvers.add(new ExcelImportArgumentResolver());
            newResolvers.addAll(resolvers);
            handlerAdapter.setArgumentResolvers(newResolvers);
        }
    }

}