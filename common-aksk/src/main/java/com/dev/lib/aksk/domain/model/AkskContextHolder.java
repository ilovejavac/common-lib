package com.dev.lib.aksk.domain.model;

public class AkskContextHolder {

    private static final ThreadLocal<AkskAuthentication> HOLDER = new ThreadLocal<>();

    private static final AkskAuthentication ANONYMOUS;

    static {
        ANONYMOUS = new AkskAuthentication();
        ANONYMOUS.setAccessKey("anonymous");
        ANONYMOUS.setSubjectName("Anonymous");
    }

    private AkskContextHolder() {
    }

    public static void set(AkskAuthentication authentication) {

        HOLDER.set(authentication);
    }

    public static AkskAuthentication get() {

        return HOLDER.get();
    }

    public static AkskAuthentication current() {

        AkskAuthentication authentication = HOLDER.get();
        return authentication == null ? ANONYMOUS : authentication;
    }

    public static void clear() {

        HOLDER.remove();
    }

    public static boolean isAuthenticated() {

        return HOLDER.get() != null;
    }
}
