package com.dev.lib.cache;

import com.dev.lib.util.Jsons;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;

public class JacksonJsonRedissonCodec extends BaseCodec {

    private final Encoder encoder = in -> {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream os = new ByteBufOutputStream(out);
            Jsons.writeWithType(os, in);
            return os.buffer();
        } catch (Exception e) {
            out.release();
            throw new IOException(e);
        }
    };

    private final Decoder<Object> decoder = (buf, state) -> Jsons.parseWithType(
            new ByteBufInputStream(buf),
            Object.class
    );

    @Override
    public Decoder<Object> getValueDecoder() {

        return decoder;
    }

    @Override
    public Encoder getValueEncoder() {

        return encoder;
    }
}
