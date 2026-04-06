package com.guiderag.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private long code;
    private String message;
    private T data;

    protected Result() {}

    protected Result(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> failed() {
        return new Result<T>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    public static <T> Result<T> failed(String message) {
        return new Result<T>(ResultCode.ERROR.getCode(), message, null);
    }
    
    public static <T> Result<T> failed(ResultCode errorCode) {
        return new Result<T>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 判断是否成功
     *
     * @return true 成功，false 失败
     */
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
