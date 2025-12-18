package com.dev.lib.util.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerichHttpGateway {

    // .header()/.body().post(object.class)
    // .headers().param().get(x.class)
    // execute("POST")

    public static GenerichHttpGateway resolve(Object object) {

        return null;
    }

}
