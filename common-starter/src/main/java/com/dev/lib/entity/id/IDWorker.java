package com.dev.lib.entity.id;

public class IDWorker {

    public static long nextID() {
        return SnowflakeConfig.getWorker().nextId();
    }

    public static String newId() {
        return IntEncoder.encode36(nextID());
    }
}