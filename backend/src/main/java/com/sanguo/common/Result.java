package com.sanguo.common;

import lombok.Data;

/**
 * 后端统一返回体（前端 request.js 强依赖此结构）
 *
 * <p>成功：code=200，data 为真实业务数据</p>
 * <p>失败：code!=200，msg 为错误原因，data 固定为 null</p>
 *
 * <p>约定：前端只要看到 401（无论是 code=401 还是 HTTP 401），就会清理 token 并跳转登录页。</p>
 */
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(400, msg);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }
}
