package com.dev.lib.jpa.entity.log;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.entity.log.OperateLog;
import com.dev.lib.security.util.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

// AOP 切面
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperateLogAspect {

    private final OperateLogRepo operateLogRepository;

    private final HttpServletRequest request;

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint point, OperateLog operateLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        OperateLogEntity log = new OperateLogEntity();
        log.setModule(operateLog.module());
        log.setType(operateLog.type());
        log.setDescription(operateLog.description());
        log.setMethod(point.getSignature().toString());
        log.setOperator(SecurityContextHolder.getUsername());
        log.setIp(SecurityContextHolder.get().getClientIp());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setOperateTime(LocalDateTime.now());
        log.setDeptId(SecurityContextHolder.get().getDeptId());
        log.setCreatorId(SecurityContextHolder.getUserId());
        log.setModifierId(SecurityContextHolder.getUserId());

        if (operateLog.recordParams()) {
            log.setRequestParams(JSON.toJSONString(point.getArgs()));
        }

        try {
            Object result = point.proceed();

            if (operateLog.recordResult()) {
                log.setResult(JSON.toJSONString(result));
            }

            log.setSuccess(true);
            log.setCostTime((int) (System.currentTimeMillis() - startTime));

            return result;
        } catch (Throwable e) {
            log.setSuccess(false);
            log.setErrorMsg(e.getMessage());
            log.setCostTime((int) (System.currentTimeMillis() - startTime));
            throw e;
        } finally {
            // 异步保存
            CompletableFuture.runAsync(() -> operateLogRepository.save(log));
        }
    }
}