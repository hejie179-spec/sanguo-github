package com.sanguo.common;

/**
 * 业务异常（用于“可预期的失败”）
 *
 * <p>用法：在业务判断不满足时主动抛出 BizException，让 GlobalExceptionHandler 统一转换成 Result.fail(code,msg)。</p>
 * <p>默认 code=400（参数/业务不满足）；如果需要区分状态可传入自定义 code。</p>
 */
public class BizException extends RuntimeException {
    private int code = 400;

    public BizException(String message) {
        super(message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
