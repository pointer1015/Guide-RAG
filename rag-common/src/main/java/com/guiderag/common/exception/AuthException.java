package com.guiderag.common.exception;

public class AuthException extends BizException {
    public AuthException(String msg) {
        super("401", msg);
    }

    public AuthException(String code, String msg) {
        super(code, msg);
    }
}
