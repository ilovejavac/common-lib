package com.dev.lib.security.web;

import com.dev.lib.security.TokenException;
import com.dev.lib.security.model.UserStatus;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UserContextHeaders {

    public static final String USER_VALIDATED = "x-user-validated";

    public static final String USER_ID = "x-user-id";

    public static final String USER_NAME = "x-user-name";

    public static final String USER_ROLE = "x-user-role";

    public static final String USER_PERMISSION = "x-user-permission";

    public static final String USER_TENANT = "x-user-tenant";

    public static final String USER_DEPT_ID = "x-user-deptId";

    public static final String USER_STATUS = "x-user-status";

    public static final String USER_CLIENT_IP = "x-user-client-ip";

    public static final String USER_CLIENT_TYPE = "x-user-client-type";

    public static final String USER_DEVICE_ID = "x-user-device-id";

    private static final Set<String> HEADER_NAMES = Set.of(
            USER_VALIDATED,
            USER_ID,
            USER_NAME,
            USER_ROLE,
            USER_PERMISSION,
            USER_TENANT,
            USER_DEPT_ID,
            USER_STATUS,
            USER_CLIENT_IP,
            USER_CLIENT_TYPE,
            USER_DEVICE_ID
    );

    private UserContextHeaders() {

    }

    public static Set<String> names() {

        return HEADER_NAMES;
    }

    public static Map<String, String> toHeaders(UserDetails userDetails) {

        if (userDetails == null || userDetails.getId() == null || !Boolean.TRUE.equals(userDetails.getValidated())) {
            return Map.of();
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(USER_VALIDATED, String.valueOf(userDetails.getValidated()));
        headers.put(USER_ID, String.valueOf(userDetails.getId()));
        putString(headers, USER_NAME, userDetails.getUsername());
        putJoined(headers, USER_ROLE, userDetails.getRoles());
        putJoined(headers, USER_PERMISSION, userDetails.getPermissions());
        putLong(headers, USER_TENANT, userDetails.getTenant());
        putLong(headers, USER_DEPT_ID, userDetails.getDeptId());
        putStatus(headers, USER_STATUS, userDetails.getStatus());
        putString(headers, USER_CLIENT_IP, userDetails.getClientIp());
        putString(headers, USER_CLIENT_TYPE, userDetails.getClientType());
        putString(headers, USER_DEVICE_ID, userDetails.getDeviceId());
        return headers;
    }

    public static Optional<UserDetails> from(HttpServletRequest request) {

        String userId = StringUtils.trimToNull(request.getHeader(USER_ID));
        if (userId == null) {
            return Optional.empty();
        }

        UserDetails userDetails = UserDetails.builder()
                .validated(parseValidated(request.getHeader(USER_VALIDATED)))
                .id(parseLong(USER_ID, userId))
                .username(StringUtils.trimToNull(request.getHeader(USER_NAME)))
                .roles(split(request.getHeader(USER_ROLE)))
                .permissions(split(request.getHeader(USER_PERMISSION)))
                .tenant(parseNullableLong(USER_TENANT, request.getHeader(USER_TENANT)))
                .deptId(parseNullableLong(USER_DEPT_ID, request.getHeader(USER_DEPT_ID)))
                .status(parseStatus(request.getHeader(USER_STATUS)))
                .clientIp(StringUtils.trimToNull(request.getHeader(USER_CLIENT_IP)))
                .clientType(StringUtils.trimToNull(request.getHeader(USER_CLIENT_TYPE)))
                .deviceId(StringUtils.trimToNull(request.getHeader(USER_DEVICE_ID)))
                .build();
        return Optional.of(userDetails);
    }

    private static void putString(Map<String, String> headers, String name, String value) {

        if (StringUtils.isNotBlank(value)) {
            headers.put(name, value);
        }
    }

    private static void putLong(Map<String, String> headers, String name, Long value) {

        if (value != null) {
            headers.put(name, String.valueOf(value));
        }
    }

    private static void putStatus(Map<String, String> headers, String name, UserStatus value) {

        if (value != null) {
            headers.put(name, value.name());
        }
    }

    private static void putJoined(Map<String, String> headers, String name, List<String> values) {

        if (values != null && !values.isEmpty()) {
            headers.put(name, String.join(",", values));
        }
    }

    private static List<String> split(String value) {

        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private static Long parseNullableLong(String name, String value) {

        String trimmed = StringUtils.trimToNull(value);
        return trimmed == null ? null : parseLong(name, trimmed);
    }

    private static Long parseLong(String name, String value) {

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new TokenException("invalid user header: " + name);
        }
    }

    private static Boolean parseValidated(String value) {

        String trimmed = StringUtils.trimToNull(value);
        if (trimmed == null) {
            return true;
        }
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        throw new TokenException("invalid user header: " + USER_VALIDATED);
    }

    private static UserStatus parseStatus(String value) {

        String trimmed = StringUtils.trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return UserStatus.valueOf(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new TokenException("invalid user header: " + USER_STATUS);
        }
    }
}
