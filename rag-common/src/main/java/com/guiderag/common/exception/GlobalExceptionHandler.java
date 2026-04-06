package com.guiderag.common.exception;

import com.guiderag.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public Result<String> handleBaseException(BaseException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        if (e.getResultCode() != null) {
            return Result.failed(e.getResultCode());
        }
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.failed("系统异常，请稍后再试");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("无法找到静态资源: {}", e.getResourcePath());
        return Result.failed("资源未找到");
    }
}
