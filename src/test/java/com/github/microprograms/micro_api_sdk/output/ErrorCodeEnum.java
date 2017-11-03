package com.github.microprograms.micro_api_sdk.output;

import com.github.microprograms.micro_api_runtime.model.ResponseCode;

public enum ErrorCodeEnum implements ResponseCode {

    /**资源不存在或已被删除*/
    not_exists(1010, "资源不存在或已被删除"), /**字符串太长或太短*/
    too_long_or_too_short(1011, "字符串太长或太短");

    private ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;

    public int getCode() {
        return code;
    }

    private final String message;

    public String getMessage() {
        return message;
    }
}
