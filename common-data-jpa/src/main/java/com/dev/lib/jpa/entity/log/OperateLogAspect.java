package com.dev.lib.jpa.entity.log;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.entity.log.OperateLog;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.Dispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
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

        OperateLogEntity logger = new OperateLogEntity();
        logger.setModule(operateLog.module());
        logger.setType(operateLog.type());
        logger.setDescription(operateLog.description());
        logger.setMethod(point.getSignature().toString());
        logger.setOperator(SecurityContextHolder.getUsername());
        logger.setIp(Optional.ofNullable(SecurityContextHolder.get()).map(UserDetails::getClientIp).orElse(""));
        logger.setUserAgent(request.getHeader("User-Agent"));
        logger.setOperateTime(LocalDateTime.now());
        logger.setDeptId(Optional.ofNullable(SecurityContextHolder.get()).map(UserDetails::getDeptId).orElse(null));
        logger.setCreatorId(SecurityContextHolder.getUserId());
        logger.setModifierId(SecurityContextHolder.getUserId());

        if (operateLog.recordParams()) {
            Object[] filteredArgs = java.util.Arrays.stream(point.getArgs())
                    .filter(arg -> !(arg instanceof org.springframework.web.multipart.MultipartFile))
                    .toArray();
            logger.setRequestParams(JSON.toJSONString(filteredArgs));
        }

        try {
            Object result = point.proceed();

            if (operateLog.recordResult()) {
                logger.setResult(JSON.toJSONString(result));
            }

            logger.setSuccess(true);

            return result;
        } catch (Throwable e) {
            logger.setSuccess(false);
            logger.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            logger.setCostTime((int) (System.currentTimeMillis() - startTime));
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            operateLogRepository.save(logger);
                        } catch (Exception e) {
                            log.warn("操作日志保存失败: {}", e.getMessage());
                        }
                    }, Dispatcher.IO
            );
        }
    }

}