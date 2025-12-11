package com.dev.lib.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageUtils {

    private static MessageSource messageSource;

    @Autowired
    public void setMessageSource(MessageSource messageSource) {

        MessageUtils.messageSource = messageSource;
    }

    public static String get(String code, Object... args) {

        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                code,
                args,
                code,
                locale
        );  // 找不到返回 code 本身
    }

    public static String get(String code) {

        return get(
                code,
                (Object[]) null
        );  // 避免歧义
    }

}