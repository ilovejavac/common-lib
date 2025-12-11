package com.dev.lib.encrypt;

import com.dev.lib.entity.encrypt.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptUtil {

    private static EncryptionService eC;

    @Autowired
    public void set(EncryptionService encryptionService) {

        eC = encryptionService;
    }

    public static String encrypt(String value) {

        return eC.encrypt(value);
    }

    public static String decrypt(String value) {

        return eC.decrypt(value);
    }

}
