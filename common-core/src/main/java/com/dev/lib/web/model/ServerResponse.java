package com.dev.lib.web.model;

import com.dev.lib.util.Jsons;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Integer SUCCESS_CODE = 200;

    private static final String SUCCESS_MESSAGE = "success";

    private static final String DEFAULT_FAILURE_MESSAGE = "failed";

    private static final String TRACE_ID_KEY = "trace_id";

    private static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";

    private Integer code;

    private String message;

    private String error;

    private T data;

    private PageResult pager;

    private Long timestamp = System.currentTimeMillis();

    private String traceId = MDC.get(TRACE_ID_KEY);

    public static <T> ServerResponse<T> ok() {

        return response(SUCCESS_CODE, SUCCESS_MESSAGE, null, null, null);
    }

    public static <T> ServerResponse<T> success(T data) {

        return response(SUCCESS_CODE, SUCCESS_MESSAGE, null, data, null);
    }

    public static <T> ServerResponse<List<T>> success(Page<T> page) {

        if (page == null) {
            return response(SUCCESS_CODE, SUCCESS_MESSAGE, null, null, null);
        }
        return response(SUCCESS_CODE, SUCCESS_MESSAGE, null, page.getContent(), pager(page));
    }

    private static PageResult pager(Page<?> page) {

        PageResult pager = new PageResult();
        pager.setPage(page.getPageable().getPageNumber() + 1);
        pager.setSize(page.getPageable().getPageSize());
        pager.setTotal(page.getTotalElements());
        pager.setHasNext(page.hasNext());
        return pager;
    }

    public static ServerResponse<Void> fail(CodeEnums codeEnums) {

        Objects.requireNonNull(codeEnums, "codeEnums must not be null");
        return fail(codeEnums.getCode(), codeEnums.getMessage());
    }

    public static ServerResponse<Void> fail(Integer code, String message) {

        return fail(code, message, null);
    }

    public static <T> ServerResponse<T> fail(Integer code, String message, T data) {

        return failure(code, message, data);
    }

    public static <T> ServerResponse<T> requestFail(Integer code, String message, T data) {

        return failure(code, message, data);
    }

    public void to(HttpServletResponse response) {

        Objects.requireNonNull(response, "response must not be null");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(APPLICATION_JSON_UTF8);
        try {
            var writer = response.getWriter();
            writer.write(Jsons.toJson(this));
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write server response", e);
        }
    }

    private static <T> ServerResponse<T> failure(Integer code, String message, T data) {

        String value = message != null ? message : DEFAULT_FAILURE_MESSAGE;
        return response(code, value, value, data, null);
    }

    private static <T> ServerResponse<T> response(
            Integer code,
            String message,
            String error,
            T data,
            PageResult pager
    ) {

        ServerResponse<T> result = new ServerResponse<>();
        result.setCode(code);
        result.setMessage(message);
        result.setError(error);
        result.setData(data);
        result.setPager(pager);
        return result;
    }

}
