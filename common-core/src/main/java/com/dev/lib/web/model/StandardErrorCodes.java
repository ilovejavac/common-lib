package com.dev.lib.web.model;

/**
 * Framework-level error code ranges.
 * Business modules may keep their own domain codes, but cross-cutting framework
 * handlers should stay inside these buckets so clients can reason about them.
 */
public final class StandardErrorCodes {

    private StandardErrorCodes() {

    }

    // 41xx: request and validation
    public static final int VALIDATION_FAILED = 4101;
    public static final int BIND_FAILED = 4102;
    public static final int PARAM_MISSING = 4103;
    public static final int PARAM_TYPE_INVALID = 4104;
    public static final int PARAM_INVALID = 4105;
    public static final int HEADER_MISSING = 4106;
    public static final int PATH_VARIABLE_MISSING = 4107;
    public static final int REQUEST_BODY_INVALID = 4108;

    // 42xx: authentication and authorization
    public static final int AUTHENTICATION_FAILED = 4201;
    public static final int TOKEN_INVALID = 4202;
    public static final int PERMISSION_DENIED = 4203;

    // 43xx: routing and HTTP capability
    public static final int API_NOT_FOUND = 4304;
    public static final int METHOD_NOT_ALLOWED = 4305;
    public static final int MEDIA_TYPE_UNSUPPORTED = 4306;
    public static final int MEDIA_TYPE_NOT_ACCEPTABLE = 4307;
    public static final int FILE_SIZE_EXCEEDED = 4308;
    public static final int REQUEST_TIMEOUT = 4309;

    // 51xx: persistence and concurrency
    public static final int DUPLICATE_KEY = 5101;
    public static final int DATA_INTEGRITY_VIOLATION = 5102;
    public static final int DATABASE_ERROR = 5103;
    public static final int CONCURRENT_MODIFICATION = 5104;

    // 55xx: system failures
    public static final int SYSTEM_ERROR = 5500;
    public static final int SYSTEM_STATE_ERROR = 5501;
}
