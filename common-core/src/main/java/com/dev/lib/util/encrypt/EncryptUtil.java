package com.dev.lib.util.encrypt;

import com.dev.lib.entity.encrypt.EncryptionService;
import com.dev.lib.util.Jsons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptUtil {

    private static EncryptionService eC;

    @Autowired
    public void set(EncryptionService encryptionService) {

        eC = encryptionService;
    }

    public static String encrypt(Object o) {
        if (o == null) return null;
        return encrypt(Jsons.toJson(o));
    }

    public static String encrypt(String value) {

        return eC.encrypt(value);
    }

    public static String decrypt(String value) {

        return eC.decrypt(value);
    }

}
