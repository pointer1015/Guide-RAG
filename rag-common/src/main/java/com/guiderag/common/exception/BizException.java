package com.guiderag.common.exception;

public class BizException extends RuntimeException {
    private final String code;
    private final String msg;

    public BizException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.code = errorCodeEnum.getCode();
        this.msg = errorCodeEnum.getMessage();
    }

    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
