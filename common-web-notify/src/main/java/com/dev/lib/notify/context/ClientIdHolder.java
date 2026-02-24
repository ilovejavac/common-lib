package com.dev.lib.notify.context;

/**
 * Client ID ThreadLocal 上下文
 * 存储当前请求的客户端 ID
 */
public class ClientIdHolder {

    private static final ThreadLocal<String> CLIENT_ID = new ThreadLocal<>();

    private ClientIdHolder() {
    }

    /**
     * 设置当前客户端 ID
     */
    public static void setClientId(String clientId) {
        CLIENT_ID.set(clientId);
    }

    /**
     * 获取当前客户端 ID
     */
    public static String getClientId() {
        return CLIENT_ID.get();
    }

    /**
     * 清除当前客户端 ID
     */
    public static void clear() {
        CLIENT_ID.remove();
    }
}
