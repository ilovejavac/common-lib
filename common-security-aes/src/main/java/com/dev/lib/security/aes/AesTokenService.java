package com.dev.lib.security.aes;

import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.Jsons;
import com.dev.lib.util.encrypt.EncryptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AesTokenService implements TokenService {

    @Override
    public String generateToken(UserDetails userDetails) {

        return EncryptUtil.encrypt(userDetails);
    }

    @Override
    public UserDetails parseToken(String token) {

        return Jsons.parse(EncryptUtil.decrypt(token), UserDetails.class);
    }

}
