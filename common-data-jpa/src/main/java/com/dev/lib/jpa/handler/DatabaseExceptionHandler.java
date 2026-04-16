package com.dev.lib.jpa.handler;

import com.dev.lib.web.MessageUtils;
import com.dev.lib.web.model.ServerResponse;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Configuration
@RestControllerAdvice
public class DatabaseExceptionHandler {

    /**
     * 唯一键/主键冲突
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ServerResponse<Void> handleDuplicateKey(DuplicateKeyException e, HttpServletRequest request) {

        log.warn("唯一键冲突", e);
        return ServerResponse.fail(StandardErrorCodes.DUPLICATE_KEY, MessageUtils.get("error.duplicate.key"), null);
    }

    /**
     * 数据完整性约束（外键等）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ServerResponse<Void> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {

        log.warn("数据完整性约束违反 [{}]", request.getRequestURI(), e);
        return ServerResponse.fail(StandardErrorCodes.DATA_INTEGRITY_VIOLATION, MessageUtils.get("error.data.integrity"), null);
    }

    /**
     * SQL语法错误 / 表不存在
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public ServerResponse<Void> handleBadSql(BadSqlGrammarException e, HttpServletRequest request) {

        log.error("SQL语法错误 [{}] SQL={}", request.getRequestURI(), e.getSql(), e);
        return ServerResponse.fail(StandardErrorCodes.DATABASE_ERROR, MessageUtils.get("error.database"), null);
    }

    /**
     * 乐观锁冲突
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ServerResponse<Void> handleOptimisticLock(OptimisticLockingFailureException e, HttpServletRequest request) {

        log.warn("乐观锁冲突 [{}]", request.getRequestURI(), e);
        return ServerResponse.fail(StandardErrorCodes.CONCURRENT_MODIFICATION, MessageUtils.get("error.concurrent.modify"), null);
    }

    /**
     * 数据访问异常（兜底）
     */
    @ExceptionHandler(DataAccessException.class)
    public ServerResponse<Void> handleDataAccess(DataAccessException e, HttpServletRequest request) {

        log.error("数据访问异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return ServerResponse.fail(StandardErrorCodes.DATABASE_ERROR, MessageUtils.get("error.database"), null);
    }
}
