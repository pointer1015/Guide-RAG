package com.guiderag.common.exception;

public enum ErrorCodeEnum {
    SUCCESS("00000", "OK"),
    USER_CLIENT_ERROR("A0001", "用户端通用错误"),
    VALIDATION_ERROR("A0400", "请求参数格式校验失败"),
    UNAUTHORIZED("A0200", "未鉴权或 Token 缺失"),
    INVALID_CREDENTIALS("A0210", "账号或密码错误"),
    RESOURCE_NOT_FOUND("A0500", "知识库、文档或会话不存在"),
    SYSTEM_EXECUTION_ERROR("B0001", "系统内部运行时异常");

    private final String code;
    private final String message;

    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
