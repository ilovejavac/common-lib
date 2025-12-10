package com.dev.lib.util;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.function.Function;

public final class PageUtil {

    private PageUtil() {
    }

    public static <S, R> Slice<R> map(Slice<S> source, Function<S, R> converter) {
        return new SliceImpl<>(
                source.getContent().stream().map(converter).toList(),
                source.getPageable(),
                source.hasNext()
        );
    }
}
