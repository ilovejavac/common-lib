package com.dev.lib.cloud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;
import java.util.stream.Collectors;

public class HeaderRewriteRequestWrapper extends HttpServletRequestWrapper {

    private final Set<String>         removeHeaders;
    private final Map<String, String> addHeaders;

    public HeaderRewriteRequestWrapper(
            HttpServletRequest request,
            Set<String> removeHeaders,
            Map<String, String> addHeaders
    ) {
        super(request);
        this.removeHeaders = removeHeaders.stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        this.addHeaders = normalize(addHeaders);
    }

    @Override
    public String getHeader(String name) {
        String key = name.toLowerCase(Locale.ROOT);

        if (addHeaders.containsKey(key)) {
            return addHeaders.get(key);
        }

        if (removeHeaders.contains(key)) {
            return null;
        }

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String key = name.toLowerCase(Locale.ROOT);

        if (addHeaders.containsKey(key)) {
            return Collections.enumeration(List.of(addHeaders.get(key)));
        }

        if (removeHeaders.contains(key)) {
            return Collections.emptyEnumeration();
        }

        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>();

        Enumeration<String> originalNames = super.getHeaderNames();
        while (originalNames.hasMoreElements()) {
            String name = originalNames.nextElement();
            String key = name.toLowerCase(Locale.ROOT);

            if (!removeHeaders.contains(key) && !addHeaders.containsKey(key)) {
                names.add(name);
            }
        }

        names.addAll(addHeaders.keySet());

        return Collections.enumeration(names);
    }

    private Map<String, String> normalize(Map<String, String> headers) {

        Map<String, String> normalized = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (value != null) {
                normalized.put(name.toLowerCase(Locale.ROOT), value);
            }
        });
        return normalized;
    }
}
