package com.dev.lib.i18n;

import com.dev.lib.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Component
public class HeaderLocaleResolver implements LocaleResolver {

    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    @Override
    @NonNull
    public Locale resolveLocale(HttpServletRequest request) {

        String language = request.getHeader("Accept-Language");

        if (StringUtils.isBlank(language)) {
            return DEFAULT_LOCALE;
        }

        try {
            return Locale.forLanguageTag(language);  // zh-CN, en-US
        } catch (Exception e) {
            return DEFAULT_LOCALE;
        }
    }

    @Override
    public void setLocale(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            Locale locale
    ) {

        throw new UnsupportedOperationException("Cannot change locale - use Accept-Language header");
    }

}