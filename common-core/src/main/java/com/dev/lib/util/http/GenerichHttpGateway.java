package com.dev.lib.util.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerichHttpGateway {
    // .header()/.body().post(object.class)
    // .headers().param().get(x.class)
    // execute("POST")
    private final OkHttpClient client;

    public static GenerichHttpGateway resolve(Object object) {
        return null;
    }
}
