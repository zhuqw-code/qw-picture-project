package com.zqw.qwpicturebackend.exception;

import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 添加 错误码字段
     */
    private int code;

    /**
     * 基于 错误码和错误信息的构造器
     *
     * @param code    错误码
     * @param message 用户自定义错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
    }

    /**
     * 基于 错误码和错误信息的构造器
     *
     * @param errorCode 封装后的错误
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 基于 错误码和错误信息的构造器
     *
     * @param errorCode 错误码
     * @param message   用户自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
