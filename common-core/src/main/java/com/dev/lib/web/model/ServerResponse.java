package com.dev.lib.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse<T> implements Serializable {

    private Integer code;

    private String message;

    private String error;

    private T data;

    private PageResult pager;

    private Long timestamp = System.currentTimeMillis();

    private String traceId = MDC.get("trace_id");

    private static final String SUCCESS = "success";

    public static <T> ServerResponse<T> ok() {

        ServerResponse<T> result = new ServerResponse<>();

        result.setCode(200);
        result.setMessage(SUCCESS);
        result.setData(null);

        return result;
    }

    public static <T> ServerResponse<T> success(T data) {

        ServerResponse<T> result = new ServerResponse<>();

        result.setCode(200);
        result.setMessage(SUCCESS);
        result.setData(data);

        return result;
    }

    public static <T> ServerResponse<List<T>> success(Page<T> page) {

        ServerResponse<List<T>> result = new ServerResponse<>();
        if (page == null) {
            return result;
        }
        result.setData(page.getContent());

        setPage(page, result);

        return result;
    }

    private static void setPage(Page<?> page, ServerResponse<?> result) {

        result.setCode(200);
        result.setMessage(SUCCESS);

        PageResult pager = new PageResult();
        pager.setPage(page.getPageable().getPageNumber() + 1);
        pager.setSize(page.getPageable().getPageSize());
        pager.setTotal(page.getTotalElements());
        pager.setHasNext(page.hasNext());
        result.setPager(pager);
    }

    public static ServerResponse<Void> fail(CodeEnums codeEnums) {

        return fail(codeEnums.getCode(), codeEnums.getMessage());
    }

    public static ServerResponse<Void> fail(Integer code, String message) {

        if (1_000_000 < code || code < 100_000) {
            throw new RuntimeException("异常 code 不符合标准[ABBCCC]");
        }
        return fail(code, message, null);
    }

    public static <T> ServerResponse<T> fail(Integer code, String message, T data) {

        ServerResponse<T> result = new ServerResponse<>();

        result.setCode(code);
        result.setMessage("server failed");
        result.setError(message);
        result.setData(data);

        return result;
    }

    public static <T> ServerResponse<T> requestFail(Integer code, String message, T data) {

        ServerResponse<T> result = new ServerResponse<>();

        result.setCode(code);
        result.setMessage("request failed");
        result.setError(message);
        result.setData(data);

        return result;
    }

}