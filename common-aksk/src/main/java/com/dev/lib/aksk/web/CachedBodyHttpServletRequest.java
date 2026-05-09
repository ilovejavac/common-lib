package com.dev.lib.aksk.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {

        super(request);
        this.cachedBody = cachedBody == null ? new byte[0] : Arrays.copyOf(cachedBody, cachedBody.length);
    }

    public byte[] getCachedBody() {

        return Arrays.copyOf(cachedBody, cachedBody.length);
    }

    @Override
    public ServletInputStream getInputStream() {

        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {

        return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding()));
    }

    @Override
    public int getContentLength() {

        return cachedBody.length;
    }

    @Override
    public long getContentLengthLong() {

        return cachedBody.length;
    }

    private Charset characterEncoding() {

        String encoding = getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream source;

        private CachedBodyServletInputStream(byte[] body) {

            this.source = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {

            return source.available() == 0;
        }

        @Override
        public boolean isReady() {

            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

            if (readListener == null) {
                return;
            }
            try {
                readListener.onDataAvailable();
                if (isFinished()) {
                    readListener.onAllDataRead();
                }
            } catch (IOException ex) {
                readListener.onError(ex);
            }
        }

        @Override
        public int read() {

            return source.read();
        }
    }
}
