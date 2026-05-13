package com.sanguo.common;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理：把后端抛出的异常统一转换成 Result.fail(...)，避免前端拿到 HTML 错误页。
 *
 * <p>常见映射：</p>
 * <ul>
 *   <li>BizException：业务可预期错误（自定义 code + msg）</li>
 *   <li>AccessDeniedException：已登录但权限不足（403）</li>
 *   <li>参数校验异常：400</li>
 *   <li>其他未捕获异常：500</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.fail(403, "无权限访问");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.fail(400, msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e, HttpServletRequest request) {
        e.printStackTrace();
        return Result.fail(500, e.getMessage() != null ? e.getMessage() : "服务器错误");
    }
}
