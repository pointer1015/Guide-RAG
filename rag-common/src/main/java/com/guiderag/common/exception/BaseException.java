package com.guiderag.common.exception;

import com.guiderag.common.result.ResultCode;

public class BaseException extends RuntimeException {
    private ResultCode resultCode;

    public BaseException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BaseException(String message) {
        super(message);
    }

    public BaseException(Throwable cause) {
        super(cause);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
